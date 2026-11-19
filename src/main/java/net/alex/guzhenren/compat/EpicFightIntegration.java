package net.alex.guzhenren.compat;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.CrashBreach;
import net.alex.guzhenren.gameplay.path.strength.CrashDash;
import net.alex.guzhenren.gameplay.path.strength.CrashSources;
import net.alex.guzhenren.gameplay.path.strength.CrashStrike;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
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
import yesman.epicfight.api.animation.TransformSheet;
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
import yesman.epicfight.api.event.types.animation.StartActionEvent;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.api.event.types.player.ModifyComboCounter;
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
 * Epic Fight 双端动画注册与服务端战斗、耐力、冲撞及动作同步桥接。
 * <ul>
 *   <li>⚠ 耐力系统的当前值、自然回复、HUD 与常规消耗完全由 Epic Fight 管理；本模组仅按玩家资质[Aptitude]或十绝体缩放耐
 *       力上限（{@link #refresh}）并免除不死之躯（{@link BodyService#isUndead}）的技能耐力消耗
 *       （{@link #onSkillConsume}），不另建耐力附件或疾跑跳跃扣费逻辑。</li>
 *   <li>目标过滤：{@link #onSetTarget} 阻止锁定野生蛊（{@link WildGuEntity}）。</li>
 *   <li>连招轮[Combo Round]挂点，只认 Epic Fight 战斗模式下的空手拳击（类别为 {@code FIST} 且
 *       {@link AttackDamageService#isFistHand} 成立的连击、冲刺攻击或空中攻击，排除拳套、副手镜像挥击与武器固有技能）：
 *       {@link #onStartAction} 在出招起播时按 {@link #isFinisher} 判定收尾招（自动攻击动作表最后一段连击，或冲刺、空中
 *       攻击；不在动作表里的连击动作按一下一轮算），调 {@link ComboRound#beginStrike} 并通知
 *       {@link QiWaveService#onStrike} 配对远程拳；{@link #onModifyComboCounter} 在连段计数从非零被清零时调
 *       {@link ComboRound#abandon}，若清零正是刚起播的冲刺或空中收尾招引起则只断苦力连击、保留这一招（Epic Fight 的清零与
 *       起播事件先后不定）；{@link #onAttackPhaseEnd} 在
 *       收尾招最后一段判定结束时调 {@link ComboRound#complete}，若正常结束、未改由远程拳出手且本轮掷中
 *       （{@link ComboRound#rollsImpact}），先放范围冲击（{@link AreaImpact#release}，跳过本段已判定过的实体）与冲击环
 *       （{@link RingConeEmitter#areaImpactRing}）及命中音效。</li>
 *   <li>⚠ 收尾招的每一记命中都按「最后一下」算：Epic Fight 空手的三个收尾动作各只有一段判定，若日后收尾动作带多段判定，
 *       前几段命中也会被当成收尾。</li>
 *   <li>冲撞冲刺（{@link #dash}）：客户端上报模型朝向 {@code yRot} 与水平键盘轴 {@code vertical}/{@code horizontal}，由
 *       {@link #getDashClip} 选择前撞、背撞、左肩撞或右肩撞动画，并通过 {@link #getRingYawOffset} 还原实际水平冲刺方向
 *       （仅含后退的输入翻转 180°）；若玩家具备破障硬度上限，服务端在播放动画前先调用 {@link CrashBreach#clear} 沿路清
 *       障，并登记起点弹坑与终点打击（{@link CrashStrike#start}）。</li>
 *   <li>{@link DashAnimation} 唯一在双端共跑的位移控制：因 Epic Fight 仅在所属客户端按动作动画移动玩家再上报坐标，故两
 *       端同时读取已同步的力量数据（{@link CrashSources#of}），将坐标位移缩放 {@link #getDashScale}，并通过
 *       {@code PLAY_SPEED_MODIFIER} 将播放速度乘以 {@link CrashSources#getPlaySpeed} 后经 {@link CrashDash#limitSpeed}
 *       限制单 tick 最大位移。</li>
 *   <li>⚠ {@link DashAnimation} 必须关闭 {@code MOVE_ON_LINK}（防止过渡帧额外追加位移）、{@code AFFECT_SPEED}（防止疾跑
 *       或药水倍乘冲刺距离）并开启 {@code STOP_MOVEMENT} 与 {@code REMOVE_DELTA_MOVEMENT}（清除起跳或疾跑残留惯性），且
 *       在构建过渡链接阶段（{@code prevElapsedTime != elapsedTime}）原样返回基准速度以确保动画从第 0 秒起播，从而保证实
 *       际冲刺距离严格等于 {@link CrashSources#getTravel} 并与服务端清障通道对齐。</li>
 *   <li>全身动作动画：蓄力蛊[Store Strength Gu]原地蓄力调用 {@link #holdCharge}（锁定移动转向并禁止连击与技能）、出拳调
 *       用 {@link #strikeCharge}、中断调用 {@link #cancelCharge}；二段飞行[Second-Stage Flight]循环
 *       （{@link #getSecondStageFlightLoop}）固定头部旋转，撞墙时由服务端调用 {@link #crashFlight} 播放反弹主帧动画并短
 *       暂冻结常态动作切换；坠击下落循环（{@link #getPlungeFall}）在落差达标落地时由服务端调用 {@link #landPlunge} 播放
 *       落地主帧动画，在前 {@link #PLUNGE_LAND_LOCK} 秒内锁定移动与攻击。</li>
 *   <li>上半身叠加动画：护盾展开（{@link #raiseShield}）、投射物挥臂（{@link #throwProjectile}，上一个挥臂片段还没播完
 *       就按横扫、左直拳、右直拳续接，播完则从横扫重来，窗口取片段自身时长）与凡蛊通用伸手催动（{@link #useGu}）均配置在
 *       {@code HIGH} 叠加层与 {@code chest_arms} 遮罩下，仅通过 {@code playAnimationInClientSide} 下发给可见客户端而不
 *       进入服务端单轨动画器，以免打断正在执行的全身动作。</li>
 *   <li>{@link #playsReach} 负责过滤通用伸手动画：同一游戏刻已播放护盾或投掷专属动画（记录在 {@link #ownClips}）、处于
 *       打坐状态（{@link #isMeditating}）或由自动催动触发（{@link AutoUse#isStarting}）时均跳过通用伸手。</li>
 *   <li>打坐、可移动蓄力与炼化循环：{@link #isMeditating} 在温养[Nourish]空窍、开窍[Awakening]仪式定身或
 *       灌注类蛊[Channeled Gu]注入真元时为真，由服务端每刻调用 {@link #tickMeditation} 检测状态翻转并广播入座
 *       （{@code meditate_sit}）与起身（{@code meditate_stand}）主帧动画；惯力蛊[Habitual Strength Gu]可移动蓄力
 *       （{@link #getChargeHoldMove}，期间由 {@link #onComboAttack} 拦截普通连击）与野生蛊炼化持握
 *       （{@link #getRefineLoop}）则由客户端直接映射为上半身合成层循环。</li>
 *   <li>有意超过 300 行：所有动画访问器均为 {@link #onAnimationRegistry} 一次性写入的私有静态字段并由各播放入口共享，强
 *       行拆分会导致重复注册或向包外暴露 Epic Fight 内部类型。</li>
 * </ul>
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
    private static AnimationManager.AnimationAccessor<StaticAnimation> chargeHoldMove;
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
    private static final Map<ServerPlayer, ThrowRun> throwRuns = new WeakHashMap<>();
    private static final Map<ServerPlayer, AttackAnimation> finishers = new WeakHashMap<>();
    private static final float PLUNGE_LAND_LOCK = 0.53F;
    private static final float MEDITATE_STAND_HOLD = 1.2F;
    private static final Map<ServerPlayer, Boolean> meditating = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> ownClips = new WeakHashMap<>();

    private EpicFightIntegration() {}

    public static void initialize() {
        LivingMotion.ENUM_MANAGER.registerEnumCls(Guzhenren.MOD_ID, EpicFightLivingMotions.class);
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(EpicFightIntegration::onSkillConsume, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.SET_TARGET.registerEvent(EpicFightIntegration::onSetTarget, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.COMBO_ATTACK.registerEvent(EpicFightIntegration::onComboAttack, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.MODIFY_COMBO_COUNTER.registerEvent(EpicFightIntegration::onModifyComboCounter,
                Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.START_ACTION.registerEvent(EpicFightIntegration::onStartAction,
                Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.registerEvent(EpicFightIntegration::onAttackPhaseEnd,
                Guzhenren.MOD_ID);
    }

    public static void onAnimationRegistry(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Guzhenren.MOD_ID, builder -> {
            dashForward = builder.nextAccessor("biped/skill/dash_forward", accessor ->
                    new DashAnimation(0.1F, accessor, (float) CrashBreach.DASH_WIDTH, 0.6F, Armatures.BIPED));
            dashBackward = builder.nextAccessor("biped/skill/dash_backward", accessor ->
                    new DashAnimation(0.1F, accessor, (float) CrashBreach.DASH_WIDTH, 0.6F, Armatures.BIPED));
            dashLeft = builder.nextAccessor("biped/skill/dash_left", accessor ->
                    new DashAnimation(0.1F, accessor, (float) CrashBreach.DASH_WIDTH, 0.6F, Armatures.BIPED));
            dashRight = builder.nextAccessor("biped/skill/dash_right", accessor ->
                    new DashAnimation(0.1F, accessor, (float) CrashBreach.DASH_WIDTH, 0.6F, Armatures.BIPED));
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
            chargeHoldMove = builder.nextAccessor("biped/living/charge_hold_move", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED));
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
        ThrowClip clip = last == null ? ThrowClip.SWEEP : getThrowClip(last.clip(), now - last.tick(), last.ticks());
        AnimationManager.AnimationAccessor<StaticAnimation> animation = getThrowAnimation(clip);
        throwRuns.put(player, new ThrowRun(clip, now, getClipTicks(animation)));
        ownClips.put(player, now);

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || animation == null || player.isDeadOrDying()) return;
        patch.playAnimationInClientSide(animation, 0.0F);
    }

    static AnimationManager.AnimationAccessor<StaticAnimation> getThrowAnimation(ThrowClip clip) {
        return switch (clip) {
            case SWEEP -> throwSweep;
            case PUNCH_RIGHT -> throwPunchRight;
            case PUNCH_LEFT -> throwPunchLeft;
        };
    }

    static long getClipTicks(AssetAccessor<? extends StaticAnimation> animation) {
        return animation == null ? 0L : Math.round(animation.get().getTotalTime() * Ticks.SECOND);
    }

    static ThrowClip getThrowClip(ThrowClip last, long gap, long playing) {
        if (gap < 0L || gap > playing) return ThrowClip.SWEEP;
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

    static AssetAccessor<? extends StaticAnimation> getChargeHoldMove() { return chargeHoldMove; }

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
        CrashSources sources = CrashSources.of(player);
        CrashBreach.clear(player, direction, sources.getTravel(), sources.getMaxHardness());
        patch.playAnimationSynchronized(animation, 0.0F);
        patch.setModelYRot(yRot, true);
        RingConeEmitter.dashCone(player, direction);
        CrashStrike.start(player, direction, sources);
    }

    public static double getDashScale(Player player) { return CrashSources.of(player).getScale(); }

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

    private static void onComboAttack(ComboAttackEvent event) {
        if (StoreStrength.isMobile(event.getPlayerPatch().getOriginal())) event.cancel();
    }

    //region Combo Round [连招轮]
    private static void onStartAction(StartActionEvent event) {
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch patch)) return;
        ServerPlayer player = patch.getOriginal();
        if (!(event.getAnimation().get() instanceof AttackAnimation attack) || !isFistStrike(patch, attack)) {
            if (finishers.remove(player) != null) ComboRound.complete(player, true);
            return;
        }

        boolean finishing = isFinisher(attack, patch.getPrimaryItemCapability().getAutoAttackMotion(patch));
        if (finishing) finishers.put(player, attack);
        else finishers.remove(player);
        ComboRound.beginStrike(player, finishing);
        QiWaveService.onStrike(player, finishing);
    }

    private static void onModifyComboCounter(ModifyComboCounter event) {
        if (event.getPrevValue() <= 0 || event.getNextValue() != 0) return;
        ServerPlayer player = event.getPlayerPatch().getOriginal();
        if (event.getAnimation() != null && finishers.get(player) == event.getAnimation().get()) {
            ComboRound.breakStreak(player);
        } else {
            ComboRound.abandon(player);
        }
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch patch)) return;
        AttackAnimation animation = event.getAnimation().get();
        ServerPlayer player = patch.getOriginal();
        if (finishers.get(player) != animation || event.getPhaseOrder() < animation.phases.length - 1) return;

        finishers.remove(player);
        if (!event.isAnimationTerminated() && isFistStrike(patch, animation) && !ComboRound.isRanged(player)
                && ComboRound.rollsImpact(player)) {
            releaseImpact(patch, event.getPhase());
        }
        ComboRound.complete(player, event.isAnimationTerminated());
    }

    private static void releaseImpact(ServerPlayerPatch patch, AttackAnimation.Phase phase) {
        ServerPlayer player = patch.getOriginal();
        Vec3 look = player.getLookAngle();
        RingConeEmitter.areaImpactRing(player, look, AreaImpact.getRange(AreaImpact.getPoints(player)));
        List<LivingEntity> landed = AreaImpact.release(player, look, patch.getCurrentlyAttackTriedEntities());
        SoundEvent sound = phase.getProperty(AttackPhaseProperty.HIT_SOUND)
                .orElseGet(() -> patch.getWeaponHitSound(phase.effectiveHand(patch)));
        if (sound == null) return;

        ServerLevel level = player.serverLevel();
        for (LivingEntity target : landed) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(),
                    1.0F, 1.0F);
        }
    }

    private static boolean isFistStrike(ServerPlayerPatch patch, AttackAnimation animation) {
        return patch.isEpicFightMode() && isFistStrike(animation)
                && patch.getPrimaryItemCapability().getWeaponCategory() == CapabilityItem.WeaponCategories.FIST
                && AttackDamageService.isFistHand(patch.getOriginal());
    }

    private static boolean isFistStrike(AttackAnimation animation) {
        return animation instanceof ComboAttackAnimation || animation instanceof DashAttackAnimation
                || animation instanceof AirSlashAnimation;
    }

    static boolean isFinisher(AttackAnimation animation,
                              List<? extends AssetAccessor<? extends AttackAnimation>> motions) {
        if (!(animation instanceof ComboAttackAnimation)) return true;
        int combos = motions.size() - 2;
        for (int i = 0; i < combos; i++) {
            if (motions.get(i).get() == animation) return isLastCombo(i, motions.size());
        }
        return true;
    }

    static boolean isLastCombo(int index, int motions) { return index == motions - 3; }
    //endregion

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

    private record ThrowRun(ThrowClip clip, long tick, long ticks) {}

    private static final class DashAnimation extends DodgeAnimation {

        private DashAnimation(float transitionTime, AnimationManager.AnimationAccessor<DodgeAnimation> accessor,
                              float width, float height, AssetAccessor<? extends Armature> armature) {
            super(transitionTime, accessor, width, height, armature);
            this.addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
            this.addProperty(ActionAnimationProperty.AFFECT_SPEED, false);
            this.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true);
            this.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true);
            this.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, this::getDashSpeed);
        }

        private float getDashSpeed(DynamicAnimation animation, LivingEntityPatch<?> entityPatch, float speed,
                                   float prevElapsedTime, float elapsedTime) {
            if (prevElapsedTime != elapsedTime) return speed;
            if (!(entityPatch.getOriginal() instanceof Player player)) return speed;

            CrashSources sources = CrashSources.of(player);
            float played = (float) (speed * sources.getPlaySpeed());
            if (animation != this) return played;
            TransformSheet coord = getCoord();
            return CrashDash.limitSpeed(played, elapsedTime, sources.getScale(),
                    (from, to) -> coord.getInterpolatedTranslation((float) from)
                            .distance(coord.getInterpolatedTranslation((float) to)));
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
