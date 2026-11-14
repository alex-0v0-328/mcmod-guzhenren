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
 * 大型野兽（熊、虎）的 {@link ActionEntity} 状态机。
 * <ul>
 *   <li>子类给时序（挥击命中与结束 tick、首锁吼叫长度、趴下转卧睡与起身回待机过渡）、护甲前固定伤害、挥击击退、
 *       重击推进（含命中帧与移动物理）与动画名、是否主动索敌、白天环境动作相对权重。</li>
 *   <li>渲染剔除框按子类加高加宽，免裁人立与尾扫，仅碰撞箱替换时重算；{@link #canSwipe}：挥击共用前提，触及距离内、
 *       冷却完、可见。</li>
 *   <li>{@link HuntGoal} 无条件注册：{@code registerGoals} 在 {@code Mob} 构造器内、熊种类等子类字段赋值前运行，
 *       性情改在 tick 判；环境姿势中交战直接脱出，不经起身。</li>
 *   <li>交战首锁以吼叫宣告，不打断进行中动作；受击姿势不重启：它长于原版 10 tick 受伤窗口，重启会让持续命中把野兽锁死在
 *       硬直；按攻击者方位选受击姿势，攻击者在左则头偏右，播 {@code hurt_right}，反之亦然。</li>
 *   <li>游荡完成后掷一个环境动作，未中则不动；清醒非战斗姿势（含坐、卧、翻滚、蹭背）都留意猎物，仅入睡、起身、
 *       战斗中不狩猎；{@link StandStillGoal} 于坐、卧、睡、翻滚、蹭背时停住导航。</li>
 *   <li>{@link HuntGoal} 每 10 tick 扫描，按实体 id 错开，邻近野兽不同时扫；仅对比当前最佳更近者投射视线。</li>
 *   <li>有意超 300 行：十四种姿势、动画、环境链与野兽专属狩猎目标同属此层。</li>
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

        /** 攻击、吼叫、受击或死亡：命中与发现猎物都打断不了的姿势。 */
        private boolean isCombat() {
            return this.attack || this == ROAR || this == HURT_LEFT || this == HURT_RIGHT || this == DEATH;
        }
    }
}
