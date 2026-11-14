package net.alex.guzhenren.compat;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.CrashDash;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.CrashBreach;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * 服务端 Epic Fight 桥接。
 * <p>耐力与范围冲击：
 * <ul>
 *   <li>⚠ 耐力（当前值、回复、HUD、常规消耗）归 Epic Fight；本模组只按资质[Aptitude]缩放上限（{@link #refresh}）、免不
 *       死者技能耗耐，不存耐力附件，不加疾跑门槛、跳跃扣费、原版饥饿消耗附加或客户端 mixin。</li>
 *   <li>设定目标时排除野生蛊（{@link net.alex.guzhenren.entity.gu.WildGuEntity}）；冲刺输入在 {@code DashInput}，冲击环
 *       生成在 {@code RingConeEmitter}。</li>
 *   <li>{@link #onAttackPhaseEnd}：拳击命中窗口关闭时用玩家随机数掷一次范围冲击（{@link AreaImpact#rolls}）；每阶段只在
 *       服务端触发一次，被别的动画打断（{@code isAnimationTerminated}）不掷。</li>
 *   <li>拳击指 Epic Fight 模式下主手类别 FIST 且空手（{@link AttackDamageService#isFistHand}）的连击、冲刺攻击、空中攻
 *       击；拳套是 FIST 但非空手，空主手配副手武器是镜像模式、挥副手，固有技能是普通 {@code AttackAnimation}，不
 *       算。</li>
 *   <li>掷中则 {@link AreaImpact#release} 沿视线锥形命中，跳过本阶段已试目标；每次释放（含打空）发一道冲击环
 *       （{@link RingConeEmitter#areaImpactRing}），掷空不发；命中目标各播该击命中音（读法同 Epic Fight），不用其命中粒
 *       子，环即画面。</li>
 * </ul>
 * <p>冲刺：
 * <ul>
 *   <li>{@link #dashDirection}：payload 的 yRot 是模型朝向（后撤闪避仍面向敌人），{@link #getRingYawOffset(int)} 还原运
 *       动 yaw；客户端已把横移、斜向（±45°/±90°）计入 yaw，只有含后退的组合翻转 180°。</li>
 *   <li>冲撞类蛊冲刺均在水平面：payload 的 vertical/horizontal 是键盘轴（W/S、A/D），非世界竖直。</li>
 *   <li>{@code dash_forward}、{@code dash_right} 右肩撞，{@code dash_left} 左肩撞，{@code dash_backward} 背撞：四个自制
 *       动画，基于 Epic Fight 玩家骨架，位于 {@code assets/guzhenren/animmodels/animations/biped/skill/}；
 *       {@link #getDashClip} 含后退选背撞，含前进选前撞，纯横移选对应侧（horizontal 为 1 即左）；方向与选择由
 *       {@code DashRingYawTest} 固定。</li>
 *   <li>动画只靠 Root 旋转转身；Root 平移即位移，沿模型前轴（后撤反向），由 Epic Fight 按模型 yaw 旋转，故须留
 *       {@code setModelYRot}，斜向亦然。</li>
 *   <li>{@code DashAnimation} 把闪避坐标向量乘 {@link #getDashScale}，即 {@link CrashDash#getScale}
 *       （{@link StrengthService#isAllOut} 与兽力[Beast Strength]冲刺加成，如驰马）；这是本类唯一的客户端部分：
 *       Epic Fight 只在所属客户端按动作动画移动玩家再上报位置（{@code PlayerPatch.shouldMoveOnCurrentSide} 服务
 *       端 false，{@code LocalPlayerPatch} true），故倍率读客户端已同步的效果与力量数据。</li>
 *   <li>服务端为两猪之力读同一倍率：持有两猪时，发动画前先 {@link CrashBreach#clear} 清开路径。</li>
 * </ul>
 * <p>全身动画：
 * <ul>
 *   <li>蓄力蛊[Store Strength Gu]三动画同目录，姿势见 wiki《力量与战斗#蓄力一拳》：{@link #holdCharge}：动作动画
 *       {@code charge_hold}，锁移动转向、禁连击技能，{@code Animations.ReusableSources.CHARGING} 减速停在蓄满末
 *       帧。</li>
 *   <li>{@link #strikeCharge}：{@code charge_strike} 动作动画非攻击，无碰撞体、伤害，不结束攻击阶段；
 *       Root 位移即原有短步。</li>
 *   <li>{@link #cancelCharge}：每次未出拳就结束（客户端取消、服务端拒绝释放、蓄力 tick 丢弃）播 {@code charge_cancel}；
 *       普通静态动画，不锁，0.2 秒放臂后交回常态动作，可被打断。</li>
 *   <li>二段飞行[Second-Stage Flight]循环 {@code biped/living/second_stage_flight_loop}，0.8 秒，姿势见
 *       wiki《飞行与二段飞行》；循环静态动画，固定头部，因客户端按俯仰倾斜整个模型
 *       （{@link EpicFightClientIntegration} 经 {@link #getSecondStageFlightLoop} 映射到
 *       {@link EpicFightLivingMotions#SECOND_STAGE_FLIGHT_LOOP}）。</li>
 *   <li>撞击 {@code biped/skill/second_stage_flight_crash}，0.4 秒；Root 后滑 0.34 再回，不产生位移；
 *       {@link #crashFlight} 由服务端撞击播放（{@code EpicFightServerEvents}）并同步给所有可见客户端（含本人）：服务端
 *       撞击才结束阶段、发环和弹坑，客户端撞击事件只到本人，客户端监听别人看不到；主帧动画，时长内冻结常态动作（阶段同
 *       tick 结束，否则动作切换立即顶掉它），不锁移动转向攻击技能。</li>
 *   <li>下坠循环 {@code biped/living/plunge_fall}，0.4 秒，姿势见 wiki《坠击#身体动作》；循环静态动画，客户端经
 *       {@link #getPlungeFall} 映射到 {@link EpicFightLivingMotions#PLUNGE_FALL}，与坠击同始终，不论落地。</li>
 *   <li>落地 {@code biped/skill/plunge_land}，0.8 秒，Root 不产生位移；{@link #landPlunge} 由服务端落地播放
 *       （{@code EpicFightServerEvents}）并同步给所有可见客户端，落差 ≥ 3 格才播，更短只结束循环；主帧动画，前
 *       {@link #PLUNGE_LAND_LOCK} 秒（蹲姿）锁移动连击技能、冻结常态动作，转向自由，之后走开即打断，不动播完。</li>
 * </ul>
 * <p>上半身动画：
 * <ul>
 *   <li>护盾、投掷、催动动画都是普通静态动画，置于 {@code HIGH} 叠加层、自制遮罩
 *       {@code assets/guzhenren/animmodels/joint_mask/biped/chest_arms.json}（Epic Fight {@code arms} 加胸、头）下：只
 *       驱动胸、头、肩、臂、手，腿、Root、躯干留给基础层常态动作（含跑步前倾），不锁；基础层更高优先级动画（攻击、冲
 *       刺、落地蹲）会盖住它们；都经 {@code playAnimationInClientSide} 发给所有可见客户端（含本人），不进服务端动画器：
 *       其单轨，播放会结束正在运行的动作。</li>
 *   <li>护盾 {@code biped/skill/shield_raise}，所有护盾共用，0.6 秒，姿势见 wiki《肉体》；数据文件
 *       {@code biped/skill/data/shield_raise.json} 按 Epic Fight 物品使用动画的方式声明叠加层；{@link #raiseShield} 由
 *       {@code ShieldRaisedEvent} 播放（{@code EpicFightServerEvents}），现有与将来的护盾蛊自动获得，每次
 *       {@code ShieldService#raise}（含未消时补充）从头重播。</li>
 *   <li>投掷三动画各 0.3 秒，姿势见 wiki《实体》，首用于月刃，供以后投掷类技能：单发 {@code biped/skill/throw_sweep}；
 *       连发 {@code biped/skill/throw_punch_right} 及镜像 {@code throw_punch_left}。</li>
 *   <li>{@link #throwProjectile}：每个 {@code ProjectileThrownEvent}（{@code EpicFightServerEvents}）即每片月刃播一个；
 *       {@link #getThrowClip}：距上次超过 {@link #THROW_RUN_TICKS} tick 或首次为横扫，窗口内为直拳且换手，故连发为横
 *       扫、左、右、左，停顿后重来；间隔按游戏 tick 计，上次投掷存于 {@code throwRuns}（弱引用表，仅服务端线程，不存
 *       档）；月刃最快 0.1 秒一片，早于动画结束，新动画从手臂现位 0.05 秒混入顶替，只有连发末个播到归位。</li>
 *   <li>无专属动作的蛊催动播 {@code biped/skill/use_reach}，0.5 秒，姿势见 wiki《蛊虫》；{@link #useGu} 由每个
 *       {@code GuUsedEvent} 播放（{@code EpicFightServerEvents}），凡蛊每次成功催动都发，新蛊无需代码，被拒不发不
 *       播。</li>
 *   <li>{@link #playsReach} 跳过：同 tick 已播专属动画者（{@link #raiseShield}、{@link #throwProjectile} 在蛊效果内、事
 *       件前把游戏 tick 记入 {@code ownClips}，弱引用表，仅服务端线程；护盾蛊、月刃类蛊因此只播自己的，新的自带动画蛊也
 *       只需记 tick）；坐着者（{@link #isMeditating}，如希望蛊[Hope Gu]点击即开始坐下的仪式）；自动催动
 *       （{@code AutoUse#isStarting}，无手势）；杀招[Killing Move]施放、功倍蛊[Amplify Effect Gu]驱动是玩家动作，照
 *       常。</li>
 * </ul>
 * <p>打坐与炼化：
 * <ul>
 *   <li>打坐三动画（盘腿坐，温养[Nourish]空窍[Aperture]、开窍[Awakening]仪式、灌注类蛊[Channeled Gu]灌注真元[Essence]共
 *       用，姿势见 wiki《空窍》）：{@code biped/skill/meditate_sit} 0.8 秒坐下，{@code biped/living/meditate_loop} 一息
 *       4 秒，{@code biped/skill/meditate_stand} 0.67 秒起身。</li>
 *   <li>{@link #isMeditating} 两端共读：温养中、仪式仍定身（{@code AwakeningRitualService#holdsStill}，前 5 秒）或按住
 *       已炼化灌注类蛊（{@code TendedGuItem#isChanneling}，读各客户端皆知的使用物品）；故灌注按住即坐、松开或灌空即起，
 *       仪式放人即起，不等牌匾淡出。</li>
 *   <li>判定成立时所有可见客户端（含中途看到的）经 {@link #getMeditateLoop} 映射到
 *       {@link EpicFightLivingMotions#MEDITATE_LOOP}；{@link #tickMeditation} 每个服务端玩家 tick 运行
 *       （{@code EpicFightServerEvents}），上次结果存于 {@code meditating}（弱引用表，仅服务端线程，不存档），转真播坐
 *       下、转假播起身（{@link #getMeditationCue}），同步给所有可见客户端。</li>
 *   <li>盯判定不盯服务：任何结束（真元耗尽、升阶、命令）都起身，服务无需知情；初见已坐（登录续温养）不播坐下，濒死不播
 *       起身；坐下、起身为主帧动画，不锁移动转向攻击技能，只冻结常态动作：坐下全程，随后循环接手，起身前
 *       {@link #MEDITATE_STAND_HOLD} 秒，之后走开即打断；三者固定头部，Root 只降高度、不产生位移，碰撞箱与视线高度仍为
 *       站立值。</li>
 *   <li>炼化野生蛊持握 {@code biped/living/refine_loop}，2 秒，姿势见 wiki《蛊虫》，一循环一息；循环静态动画，客户端经
 *       {@link #getRefineLoop} 作叠加常态动作映射到 {@link EpicFightLivingMotions#REFINE_LOOP} （
 *       {@link EpicFightClientIntegration}）；数据文件 {@code biped/living/data/refine_loop.json} 置于 {@code MIDDLE}
 *       叠加层、{@code chest_arms} 遮罩下，可照常行走；服务端不发送。</li>
 *   <li>全部动画访问器只由 Epic Fight 动画注册设置一次，之后只读。</li>
 *   <li>有意超 300 行：访问器是 {@link #onAnimationRegistry} 一次写入的私有静态字段，各播放方法共读；拆类就得公开它们
 *       或重复注册，Epic Fight 类型也会漏出 {@code compat}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class EpicFightIntegration {

    private static final ResourceLocation STAMINA_MODIFIER = Guzhenren.id("epic_fight_stamina");
    private static final AttributeModifier.Operation STAMINA_OPERATION =
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashForward;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashBackward;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashLeft;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashRight;
    private static AnimationManager.AnimationAccessor<ActionAnimation> chargeHold;
    private static AnimationManager.AnimationAccessor<ActionAnimation> chargeStrike;
    private static AnimationManager.AnimationAccessor<StaticAnimation> chargeCancel;
    private static AnimationManager.AnimationAccessor<StaticAnimation> secondStageFlightLoop;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> secondStageFlightCrash;
    private static AnimationManager.AnimationAccessor<StaticAnimation> plungeFall;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> plungeLand;
    private static AnimationManager.AnimationAccessor<StaticAnimation> shieldRaise;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwSweep;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwPunchRight;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwPunchLeft;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> meditateSit;
    private static AnimationManager.AnimationAccessor<StaticAnimation> meditateLoop;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> meditateStand;
    private static AnimationManager.AnimationAccessor<StaticAnimation> useReach;
    private static AnimationManager.AnimationAccessor<StaticAnimation> refineLoop;
    static final long THROW_RUN_TICKS = 10L;
    private static final Map<ServerPlayer, ThrowRun> throwRuns = new WeakHashMap<>();
    private static final float PLUNGE_LAND_LOCK = 0.53F;
    private static final float MEDITATE_STAND_HOLD = 0.4F;
    private static final Map<ServerPlayer, Boolean> meditating = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> ownClips = new WeakHashMap<>();

    private EpicFightIntegration() {}

    public static void initialize() {
        LivingMotion.ENUM_MANAGER.registerEnumCls(Guzhenren.MOD_ID, EpicFightLivingMotions.class);
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(EpicFightIntegration::onSkillConsume, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.SET_TARGET.registerEvent(EpicFightIntegration::onSetTarget, Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.registerEvent(EpicFightIntegration::onAttackPhaseEnd,
                Guzhenren.MOD_ID);
    }

    public static void onAnimationRegistry(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Guzhenren.MOD_ID, builder -> {
            dashForward = builder.nextAccessor("biped/skill/dash_forward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashBackward = builder.nextAccessor("biped/skill/dash_backward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashLeft = builder.nextAccessor("biped/skill/dash_left", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashRight = builder.nextAccessor("biped/skill/dash_right", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            chargeHold = builder.nextAccessor("biped/skill/charge_hold", accessor ->
                    new ActionAnimation(0.15F, accessor, Armatures.BIPED)
                            .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                            .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                                    Animations.ReusableSources.CHARGING)
                            .addProperty(StaticAnimationProperty.RESET_LIVING_MOTION, LivingMotions.IDLE));
            chargeStrike = builder.nextAccessor("biped/skill/charge_strike", accessor ->
                    new ActionAnimation(0.05F, accessor, Armatures.BIPED));
            chargeCancel = builder.nextAccessor("biped/skill/charge_cancel", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            secondStageFlightLoop = builder.nextAccessor("biped/living/second_stage_flight_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            secondStageFlightCrash = builder.nextAccessor("biped/skill/second_stage_flight_crash", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, Float.MAX_VALUE)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            plungeFall = builder.nextAccessor("biped/living/plunge_fall", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED));
            plungeLand = builder.nextAccessor("biped/skill/plunge_land", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, PLUNGE_LAND_LOCK)
                            .addState(EntityState.MOVEMENT_LOCKED, true)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addState(EntityState.COMBO_ATTACKS_DOABLE, false)
                            .addState(EntityState.SKILL_EXECUTABLE, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            shieldRaise = builder.nextAccessor("biped/skill/shield_raise", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwSweep = builder.nextAccessor("biped/skill/throw_sweep", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwPunchRight = builder.nextAccessor("biped/skill/throw_punch_right", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwPunchLeft = builder.nextAccessor("biped/skill/throw_punch_left", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            meditateSit = builder.nextAccessor("biped/skill/meditate_sit", accessor ->
                    new MainFrameAnimation(0.1F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, Float.MAX_VALUE)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            meditateLoop = builder.nextAccessor("biped/living/meditate_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            meditateStand = builder.nextAccessor("biped/skill/meditate_stand", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, MEDITATE_STAND_HOLD)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            useReach = builder.nextAccessor("biped/skill/use_reach", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            refineLoop = builder.nextAccessor("biped/living/refine_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED));
        });
    }

    static AssetAccessor<? extends StaticAnimation> getSecondStageFlightLoop() { return secondStageFlightLoop; }

    public static void crashFlight(ServerPlayer player) {
        if (!player.isDeadOrDying()) play(player, secondStageFlightCrash);
    }

    static AssetAccessor<? extends StaticAnimation> getPlungeFall() { return plungeFall; }

    public static void landPlunge(ServerPlayer player) {
        if (!player.isDeadOrDying()) play(player, plungeLand);
    }

    public static void raiseShield(ServerPlayer player) {
        ownClips.put(player, player.serverLevel().getGameTime());
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || shieldRaise == null || player.isDeadOrDying()) return;
        patch.playAnimationInClientSide(shieldRaise, 0.0F);
    }

    public static void throwProjectile(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        ThrowRun last = throwRuns.get(player);
        ThrowClip clip = last == null ? ThrowClip.SWEEP : getThrowClip(last.clip(), now - last.tick());
        throwRuns.put(player, new ThrowRun(clip, now));
        ownClips.put(player, now);

        AnimationManager.AnimationAccessor<StaticAnimation> animation = switch (clip) {
            case SWEEP -> throwSweep;
            case PUNCH_RIGHT -> throwPunchRight;
            case PUNCH_LEFT -> throwPunchLeft;
        };
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || animation == null || player.isDeadOrDying()) return;
        patch.playAnimationInClientSide(animation, 0.0F);
    }

    static ThrowClip getThrowClip(ThrowClip last, long gap) {
        if (gap < 0L || gap > THROW_RUN_TICKS) return ThrowClip.SWEEP;
        return last == ThrowClip.PUNCH_LEFT ? ThrowClip.PUNCH_RIGHT : ThrowClip.PUNCH_LEFT;
    }

    public static void useGu(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || useReach == null || player.isDeadOrDying()) return;
        if (!playsReach(AutoUse.isStarting(player), isMeditating(player), ownClips.get(player),
                player.serverLevel().getGameTime())) return;
        patch.playAnimationInClientSide(useReach, 0.0F);
    }

    static boolean playsReach(boolean autoUse, boolean seated, Long ownClipTick, long now) {
        return !autoUse && !seated && (ownClipTick == null || ownClipTick != now);
    }

    static AssetAccessor<? extends StaticAnimation> getRefineLoop() { return refineLoop; }

    static AssetAccessor<? extends StaticAnimation> getMeditateLoop() { return meditateLoop; }

    public static boolean isMeditating(Player player) {
        return ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player)
                || TendedGuItem.isChanneling(player);
    }

    public static void tickMeditation(ServerPlayer player) {
        boolean seated = isMeditating(player);
        MeditationCue cue = getMeditationCue(meditating.put(player, seated), seated);
        if (cue == MeditationCue.NONE || player.isDeadOrDying()) return;
        play(player, cue == MeditationCue.SIT ? meditateSit : meditateStand);
    }

    static MeditationCue getMeditationCue(Boolean last, boolean seated) {
        if (last == null || last == seated) return MeditationCue.NONE;
        return seated ? MeditationCue.SIT : MeditationCue.STAND;
    }

    public static void holdCharge(ServerPlayer player) { play(player, chargeHold); }

    public static void strikeCharge(ServerPlayer player) { play(player, chargeStrike); }

    public static void cancelCharge(ServerPlayer player) { play(player, chargeCancel); }

    private static void play(ServerPlayer player, AssetAccessor<? extends StaticAnimation> animation) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null && animation != null) patch.playAnimationSynchronized(animation, 0.0F);
    }

    public static void dash(ServerPlayer player, int vertical, int horizontal, float yRot) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        if (!EpicFightSkills.STEP.get().isExecutableState(patch)) return;

        AnimationManager.AnimationAccessor<DodgeAnimation> animation = switch (getDashClip(vertical, horizontal)) {
            case BACKWARD -> dashBackward;
            case LEFT -> dashLeft;
            case RIGHT -> dashRight;
            case FORWARD -> dashForward;
        };
        if (animation == null) return;
        Vec3 direction = dashDirection(vertical, yRot);
        if (PathStrengthService.get(player).hasTwoBoars()) {
            CrashBreach.clear(player, direction, CrashDash.getTravel(getDashScale(player)) + CrashBreach.MARGIN);
        }
        patch.playAnimationSynchronized(animation, 0.0F);
        patch.setModelYRot(yRot, true);
        RingConeEmitter.dashCone(player, direction);
    }

    public static double getDashScale(Player player) {
        return CrashDash.getScale(StrengthService.isAllOut(player),
                PathStrengthService.get(player).getBeastDashBonus());
    }

    public static void refresh(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        if (instance == null) return;

        double bonus = getStaminaMaxPercent(player) / 100.0D;
        if (TransientModifiers.hasModifier(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION)) return;

        TransientModifiers.swap(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.clampMaxAttributes();
    }

    private static int getStaminaMaxPercent(ServerPlayer player) {
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        return physique == ExtremePhysique.NONE
                ? ApertureService.getTalent(player).getStaminaMaxPercent()
                : physique.getStaminaMaxPercent();
    }

    private static void onSkillConsume(SkillConsumeEvent event) {
        if (event.getResourceType() != Skill.Resource.STAMINA) return;
        if (event.getEntityPatch().getOriginal() instanceof Player player
                && BodyService.isUndead(player)) {
            event.setAmount(0.0F);
        }
    }

    private static void onSetTarget(SetTargetEvent event) {
        if (event.getTarget() instanceof WildGuEntity) event.cancel();
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        if (event.isAnimationTerminated()) return;
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch patch) || !patch.isEpicFightMode()) return;
        if (!isFistStrike(event.getAnimation().get())) return;
        if (patch.getPrimaryItemCapability().getWeaponCategory() != CapabilityItem.WeaponCategories.FIST) return;

        ServerPlayer player = patch.getOriginal();
        if (!AttackDamageService.isFistHand(player)) return;
        if (!AreaImpact.rolls(player, player.getRandom())) return;

        Vec3 look = player.getLookAngle();
        RingConeEmitter.areaImpactRing(player, look, AreaImpact.RANGE);
        List<LivingEntity> landed = AreaImpact.release(player, look, patch.getCurrentlyAttackTriedEntities());
        AttackAnimation.Phase phase = event.getPhase();
        SoundEvent sound = phase.getProperty(AttackPhaseProperty.HIT_SOUND)
                .orElseGet(() -> patch.getWeaponHitSound(phase.effectiveHand(patch)));
        if (sound == null) return;

        ServerLevel level = player.serverLevel();
        for (LivingEntity target : landed) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(),
                    1.0F, 1.0F);
        }
    }

    private static boolean isFistStrike(AttackAnimation animation) {
        return animation instanceof ComboAttackAnimation || animation instanceof DashAttackAnimation
                || animation instanceof AirSlashAnimation;
    }

    private static Vec3 dashDirection(int vertical, float yRot) {
        float radians = (yRot + getRingYawOffset(vertical)) * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    static float getRingYawOffset(int vertical) {
        return vertical < 0 ? 180.0F : 0.0F;
    }

    static DashClip getDashClip(int vertical, int horizontal) {
        if (vertical < 0) return DashClip.BACKWARD;
        if (vertical > 0 || horizontal == 0) return DashClip.FORWARD;
        return horizontal > 0 ? DashClip.LEFT : DashClip.RIGHT;
    }

    enum DashClip { FORWARD, BACKWARD, LEFT, RIGHT }

    enum ThrowClip { SWEEP, PUNCH_RIGHT, PUNCH_LEFT }

    enum MeditationCue { NONE, SIT, STAND }

    private record ThrowRun(ThrowClip clip, long tick) {}

    private static final class DashAnimation extends DodgeAnimation {

        private DashAnimation(float transitionTime, AnimationManager.AnimationAccessor<DodgeAnimation> accessor,
                              float width, float height, AssetAccessor<? extends Armature> armature) {
            super(transitionTime, accessor, width, height, armature);
        }

        @Override
        protected Vec3 getCoordVector(LivingEntityPatch<?> entityPatch,
                                      AssetAccessor<? extends DynamicAnimation> animation) {
            double scale = entityPatch.getOriginal() instanceof Player player ? getDashScale(player)
                    : CrashDash.COORD_SCALE;
            return super.getCoordVector(entityPatch, animation).scale(scale);
        }
    }
}
