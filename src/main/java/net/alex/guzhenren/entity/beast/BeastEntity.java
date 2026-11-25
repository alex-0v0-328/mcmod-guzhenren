package net.alex.guzhenren.entity.beast;

import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenren.entity.ActionEntity;
import net.alex.guzhenren.entity.ai.StandStillGoal;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 大型野兽（熊、虎）共用的 {@link ActionEntity} 状态机基类。
 * <ul>
 *   <li>子类提供动作时序（挥击命中与结束 tick、首锁吼叫时长、趴下转卧睡与起身回待机的过渡时长）、护甲前固定伤害、挥击击
 *       退、重击推进（含命中帧与位移物理）及动画名、是否主动索敌、白天环境动作的相对权重。</li>
 *   <li>渲染剔除框按子类额外加高加宽，防止人立拍击与甩尾被视锥裁剪，且仅在碰撞箱实例变化时重算；{@link #canSwipe} 封装
 *       挥击的公共前提（触及距离内、冷却完毕且视线可见）。</li>
 *   <li>⚠ {@link HuntGoal} 无条件注册，因为 {@code registerGoals} 在 {@code Mob} 构造器内、子类字段（如熊的种类）赋值
 *       前运行，是否主动索敌改在每 tick 的 {@link #isActiveHunter} 中判定；环境姿势下进入交战时直接切入战斗，不播放起身
 *       过渡。</li>
 *   <li>首次锁定目标时以吼叫宣告，但不打断进行中的动作；{@link Action#isCombat} 涵盖攻击、吼叫、受击与死亡四类不可被受
 *       击或发现猎物打断的姿势，由于受击姿势长于原版 10 tick 无敌帧，受击中不重置姿势以免被连续命中锁死在硬直；受击方向
 *       按攻击者相对朝向选择：攻击者在左侧时头部偏右，播放 {@code hurt_right}，反之播放 {@code hurt_left}。</li>
 *   <li>随机游荡走完后按概率进入环境动作，未掷中则原地待机；清醒的非战斗姿势（含坐、卧、翻滚、蹭背）仍继续扫描猎物，仅
 *       入睡、起身与战斗姿势暂停索敌；{@link StandStillGoal} 在环境姿势期间锁住移动导航。</li>
 *   <li>{@link HuntGoal} 每 10 tick 扫描一次，按实体 ID 错峰以避免邻近野兽同帧扫描，且仅对距离优于当前最佳候选的目标投
 *       射视线检查。</li>
 *   <li>本类有意超过 300 行：十四种姿势、动画映射、昼夜环境链与专属狩猎目标耦合紧密，集中维护更清晰。</li>
 * </ul>
 */

public abstract class BeastEntity extends ActionEntity<BeastEntity.Action> {

    public static final int SWIPE_COOLDOWN_TICKS = 20;
    public static final int HEAVY_COOLDOWN_TICKS = 80;
    public static final int HURT_ACTION_TICKS = 11;
    public static final int SIT_ACTION_TICKS = 160;
    public static final int LIE_ACTION_TICKS = 80;
    public static final int ROLL_ACTION_TICKS = 74;
    public static final int BACK_SCRATCH_ACTION_TICKS = 100;
    public static final double SWIPE_REACH = 2.5D;
    private static final double AMBIENT_REST_CHANCE = 0.35D;
    private static final double NIGHT_LIE_DOWN_CHANCE = 0.5D;
    private static final Action[] ACTIONS = Action.values();

    private static final Cooldown SWIPE = new Cooldown(SWIPE_COOLDOWN_TICKS, "BeastSwipeCooldown");
    private static final Cooldown HEAVY = new Cooldown(HEAVY_COOLDOWN_TICKS, "BeastHeavyCooldown");
    private static final List<Cooldown> COOLDOWNS = List.of(SWIPE, HEAVY);

    private long ambientEndGameTime = Long.MIN_VALUE;
    private @Nullable AABB cullSource;
    private AABB cullBox;

    protected BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
    }

    //region subclass hooks [子类钩子]
    protected abstract int getSwipeHitTick();

    protected abstract int getSwipeActionTicks();

    protected abstract int getRoarActionTicks();

    protected abstract int getLieDownActionTicks();

    protected abstract int getUpActionTicks();

    protected abstract float getSwipeDamage();

    protected abstract float getHeavyDamage();

    protected abstract double getSwipeKnockback();

    protected abstract double getSwipeUpward();

    protected abstract boolean isActiveHunter();

    protected abstract void tickHeavyAttack(long ticks);

    protected abstract Action pickDaytimeAmbient(double roll);

    protected abstract SoundEvent getRoarSound();

    protected abstract float getRoarPitch();

    protected abstract SoundEvent getStepSound();

    protected abstract double getCullExtraHeight();

    protected abstract double getCullHorizontalInflate();

    protected abstract String getHeavyAttackAnimation();
    //endregion

    //region action set [动作集]
    @Override
    protected Action[] getActionValues() { return ACTIONS; }

    @Override
    protected Action getIdleAction() { return Action.IDLE; }

    @Override
    protected Action getAlertAction() { return Action.ROAR; }

    @Override
    protected Action getDeathAction() { return Action.DEATH; }

    @Override
    protected List<Cooldown> getCooldowns() { return COOLDOWNS; }

    @Override
    protected Cooldown getCooldownOf(Action attack) { return attack == Action.ATTACK_SWIPE ? SWIPE : HEAVY; }

    @Override
    protected String getRecoverySaveKey() { return "BeastRecoveryTicks"; }

    public int getSwipeCooldown() {
        return this.getCooldown(SWIPE);
    }

    public int getHeavyCooldown() {
        return this.getCooldown(HEAVY);
    }
    //endregion

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(2, new HuntGoal(this));
        this.goalSelector.addGoal(4, new StandStillGoal(this, () -> this.getAction().isAmbient()));
    }

    @Override
    protected void onBeginAction(Action next) {
        if (next.isAttack() && this.actionTarget != null) this.faceDirection(this.getDirectionTo(this.actionTarget));
        if (next == Action.ROAR) this.playSound(this.getRoarSound(), 1.0F, this.getRoarPitch());
        this.ambientEndGameTime = next == Action.SIT
                ? this.level().getGameTime() + SIT_ACTION_TICKS : Long.MIN_VALUE;
    }

    @Override
    protected void onActionCleared() {
        this.ambientEndGameTime = Long.MIN_VALUE;
    }

    @Override
    protected boolean isFacingTargetInAction() {
        return this.getAction() == Action.ROAR;
    }

    //region action timing [动作时序]
    @Override
    protected void tickAction(Action current, long ticks) {
        if (current.isAmbient()) this.tickAmbientAction(current, ticks);
        else this.tickCombatAction(current, ticks);
    }

    private void tickCombatAction(Action current, long ticks) {
        switch (current) {
            case ROAR -> {
                if (ticks >= this.getRoarActionTicks()) this.finishAction();
            }
            case ATTACK_SWIPE -> {
                if (ticks >= this.getSwipeHitTick() && !this.hitSettled) {
                    this.strikeTarget(SWIPE_REACH, this.getSwipeDamage(), this.getSwipeKnockback(),
                            this.getSwipeUpward());
                }
                if (ticks >= this.getSwipeActionTicks()) this.finishAction();
            }
            case ATTACK_HEAVY -> this.tickHeavyAttack(ticks);
            case HURT_LEFT, HURT_RIGHT -> {
                if (ticks >= HURT_ACTION_TICKS) this.finishAction();
            }
            default -> {}
        }
    }

    private void tickAmbientAction(Action current, long ticks) {
        switch (current) {
            case SIT -> {
                if (this.level().getGameTime() >= this.ambientEndGameTime) this.finishAction();
            }
            case LIE_DOWN -> {
                if (ticks >= this.getLieDownActionTicks()) {
                    this.switchAction(Action.LIE);
                    this.ambientEndGameTime = this.level().getGameTime() + LIE_ACTION_TICKS;
                }
            }
            case LIE -> {
                if (!this.isNightTime()) this.tryBeginAction(Action.GET_UP, false);
                else if (this.level().getGameTime() >= this.ambientEndGameTime) this.switchAction(Action.SLEEP);
            }
            case SLEEP -> {
                if (!this.isNightTime()) this.tryBeginAction(Action.GET_UP, false);
            }
            case GET_UP -> {
                if (ticks >= this.getUpActionTicks()) this.finishAction();
            }
            case ROLL -> {
                if (ticks >= ROLL_ACTION_TICKS) this.finishAction();
            }
            case BACK_SCRATCH -> {
                if (ticks >= BACK_SCRATCH_ACTION_TICKS) this.finishAction();
            }
            default -> {}
        }
    }
    //endregion

    //region engagement [交战]
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (target != null && previous == null && !this.level().isClientSide() && !this.isDeadOrDying()
                && (this.getAction() == Action.IDLE || this.getAction().isAmbient())) {
            this.switchAction(Action.ROAR);
        }
    }

    @Override
    protected void reactToHurt(@Nullable LivingEntity attacker) {
        if (attacker != null && this.canAttackTarget(attacker)) this.lockTarget(attacker);
        if (!this.getAction().isCombat()) this.switchAction(this.pickDirectionalHurt(attacker));
    }

    private Action pickDirectionalHurt(@Nullable Entity attacker) {
        if (attacker == null) return Action.HURT_LEFT;
        double dx = attacker.getX() - this.getX();
        double dz = attacker.getZ() - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) return Action.HURT_LEFT;
        double radians = this.getYRot() * (Math.PI / 180.0D);
        double leftness = dx * Math.cos(radians) + dz * Math.sin(radians);
        return leftness > 0.0D ? Action.HURT_RIGHT : Action.HURT_LEFT;
    }
    //endregion

    @Override
    protected void onWanderComplete() {
        if (!this.isIdle() || this.getTarget() != null || this.isDeadOrDying() || !this.onGround()) return;
        if (this.isNightTime()) {
            if (this.random.nextDouble() < NIGHT_LIE_DOWN_CHANCE) this.tryBeginAction(Action.LIE_DOWN, false);
            return;
        }
        double roll = this.random.nextDouble();
        if (roll >= AMBIENT_REST_CHANCE) return;
        this.tryBeginAction(this.pickDaytimeAmbient(roll / AMBIENT_REST_CHANCE), false);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB source = super.getBoundingBoxForCulling();
        if (source != this.cullSource) {
            AABB box = source.inflate(this.getCullHorizontalInflate());
            this.cullBox = box.setMaxY(box.maxY + this.getCullExtraHeight());
            this.cullSource = source;
        }
        return this.cullBox;
    }

    protected boolean canSwipe(LivingEntity target) {
        return this.distanceTo(target) <= SWIPE_REACH && this.getSwipeCooldown() == 0
                && this.getSensing().hasLineOfSight(target);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

    @Override
    protected String getAnimationPrefix() { return "animation."; }

    @Override
    protected RawAnimation getActionAnimation(Action action) {
        RawAnimation clip = RawAnimation.begin();
        return switch (action) {
            case IDLE -> clip.thenLoop("animation.idle");
            case SIT -> clip.thenLoop("animation.sit");
            case LIE_DOWN -> clip.thenPlayAndHold("animation.lie_down");
            case LIE -> clip.thenLoop("animation.lie");
            case SLEEP -> clip.thenLoop("animation.sleep");
            case GET_UP -> clip.thenPlay("animation.get_up");
            case ROLL -> clip.thenPlay("animation.roll");
            case BACK_SCRATCH -> clip.thenPlay("animation.back_scratch");
            case ROAR -> clip.thenPlay("animation.roar");
            case ATTACK_SWIPE -> clip.thenPlay("animation.attack_swipe");
            case ATTACK_HEAVY -> clip.thenPlay(this.getHeavyAttackAnimation());
            case HURT_LEFT -> clip.thenPlay("animation.hurt_left");
            case HURT_RIGHT -> clip.thenPlay("animation.hurt_right");
            case DEATH -> clip.thenPlayAndHold("animation.death");
        };
    }

    private static final class HuntGoal extends Goal {

        private static final int SCAN_INTERVAL_TICKS = 10;
        private final BeastEntity beast;
        private final Predicate<LivingEntity> prey;

        private HuntGoal(BeastEntity beast) {
            this.beast = beast;
            this.prey = entity -> (entity instanceof Player || entity.getType().is(ModEntityTypeTags.PREDATOR_PREY))
                    && beast.canAttackTarget(entity);
        }

        @Override
        public boolean canUse() {
            Action action = this.beast.getAction();
            return this.beast.isActiveHunter() && this.beast.getTarget() == null
                    && action != Action.SLEEP && action != Action.GET_UP && !action.isCombat();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if ((this.beast.tickCount + this.beast.getId()) % SCAN_INTERVAL_TICKS != 0) return;
            double range = this.beast.getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB box = this.beast.getBoundingBox().inflate(range);
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : this.beast.level().getEntitiesOfClass(LivingEntity.class, box, this.prey)) {
                double distance = this.beast.distanceToSqr(candidate);
                if (distance < bestDistance && this.beast.getSensing().hasLineOfSight(candidate)) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            if (best != null) this.beast.setTarget(best);
        }
    }

    public enum Action implements ActionEntity.ActionFlags {

        IDLE(true, false, false),
        SIT(true, false, true),
        LIE_DOWN(false, false, true),
        LIE(true, false, true),
        SLEEP(true, false, true),
        GET_UP(false, false, true),
        ROLL(false, false, true),
        BACK_SCRATCH(false, false, true),
        ROAR(false, false, false),
        ATTACK_SWIPE(false, true, false),
        ATTACK_HEAVY(false, true, false),
        HURT_LEFT(false, false, false),
        HURT_RIGHT(false, false, false),
        DEATH(false, false, false);

        private final boolean loop;
        private final boolean attack;
        private final boolean ambient;

        Action(boolean loop, boolean attack, boolean ambient) {
            this.loop = loop;
            this.attack = attack;
            this.ambient = ambient;
        }

        @Override
        public boolean isLooping() { return this.loop; }

        @Override
        public boolean isAttack() { return this.attack; }

        @Override
        public boolean isAmbient() { return this.ambient; }

        private boolean isCombat() {
            return this.attack || this == ROAR || this == HURT_LEFT || this == HURT_RIGHT || this == DEATH;
        }
    }
}
