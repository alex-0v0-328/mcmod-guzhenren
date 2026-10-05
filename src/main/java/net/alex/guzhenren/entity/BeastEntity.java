package net.alex.guzhenren.entity;

import java.util.EnumSet;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * The big beasts (bears, tigers): the {@link ActionEntity} state machine with a swipe and a heavy attack, a
 * first-lock roar, directional hurt poses and the night-sleep / daytime-rest ambient chain.
 *
 * <p>Subclasses supply the timing constants: {@link #swipeHitTick} is the tick at which the swipe's hit frame
 * lands; {@link #swipeActionTicks} the tick at which the swipe ends; {@link #roarActionTicks} the length of the
 * first-lock roar; {@link #lieDownActionTicks} the lie-down transition into lie/sleep; {@link #getUpActionTicks}
 * the get-up transition back to idle. The attack: {@link #swipeDamage} and {@link #heavyDamage} are the flat
 * damage before armor; {@link #swipeKnockback} and {@link #swipeUpward} the swipe's knockback;
 * {@link #tickHeavyAttack} advances the heavy attack, hit frames and movement physics included.
 * {@link #huntsActively} says whether the beast seeks targets on its own or only retaliates;
 * {@link #pickDaytimeAmbient} picks the daytime ambient action, with relative weights;
 * {@link #cullExtraHeight} and {@link #cullHorizontalInflate} extend the render culling box so rearing poses
 * and tail sweeps are not clipped; {@link #heavyAttackAnimation} names the heavy attack's animation.
 *
 * <p>{@link HuntGoal} is registered unconditionally: {@code registerGoals} runs inside the {@code Mob}
 * constructor, before subclass fields like the bear species are assigned, so the temperament check happens
 * at tick time. A beast that becomes engaged in an ambient pose is snapped straight out of it without a
 * get-up transition.
 *
 * <p>In {@link #setTarget}, the first lock of an engagement is announced by the roar; it never interrupts a
 * running action. In {@link #reactToHurt}, a running hurt pose is never restarted: it outlasts vanilla's
 * 10-tick damage window, so restarting it would let steady hits stun-lock the beast out of every attack.
 * {@link #directionalHurt} picks the hurt pose from the attacker's bearing: an attacker on this beast's left
 * side knocks the head to the beast's right, playing {@code hurt_right}, and vice versa.
 *
 * <p>{@link #rollAmbient} picks and starts an ambient action after a completed wander; nothing happens on a
 * miss. In {@link HuntGoal#canUse}, any awake, non-combat pose notices prey -- including sitting, lying,
 * rolling and scratching; only actual sleep, the get-up transition and ongoing combat skip hunting.
 * {@link RestGoal} holds navigation still while the beast sits, lies, sleeps, rolls or scratches.
 *
 * <p>Past 300 lines on purpose: the fourteen poses, their animations, the ambient chain and the two
 * beast-only goals belong to this layer.
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
    private static final int NIGHT_START_TICK = 13000;
    private static final int NIGHT_END_TICK = 23000;
    private static final Action[] ACTIONS = Action.values();

    private static final EntityDataAccessor<Integer> DATA_SWIPE_COOLDOWN = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAVY_COOLDOWN = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.INT);
    private static final Cooldown SWIPE = new Cooldown(DATA_SWIPE_COOLDOWN, SWIPE_COOLDOWN_TICKS, "BeastSwipeCooldown");
    private static final Cooldown HEAVY = new Cooldown(DATA_HEAVY_COOLDOWN, HEAVY_COOLDOWN_TICKS, "BeastHeavyCooldown");
    private static final List<Cooldown> COOLDOWNS = List.of(SWIPE, HEAVY);

    private long ambientEndGameTime = Long.MIN_VALUE;

    protected BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
    }

    //region subclass hooks
    protected abstract int swipeHitTick();

    protected abstract int swipeActionTicks();

    protected abstract int roarActionTicks();

    protected abstract int lieDownActionTicks();

    protected abstract int getUpActionTicks();

    protected abstract float swipeDamage();

    protected abstract float heavyDamage();

    protected abstract double swipeKnockback();

    protected abstract double swipeUpward();

    protected abstract boolean huntsActively();

    protected abstract void tickHeavyAttack(long ticks);

    protected abstract Action pickDaytimeAmbient(double roll);

    protected abstract SoundEvent roarSound();

    protected abstract float roarPitch();

    protected abstract SoundEvent stepSound();

    protected abstract double cullExtraHeight();

    protected abstract double cullHorizontalInflate();

    protected abstract String heavyAttackAnimation();
    //endregion

    //region action set
    @Override
    protected Action[] actionValues() { return ACTIONS; }

    @Override
    protected Action idleAction() { return Action.IDLE; }

    @Override
    protected Action alertAction() { return Action.ROAR; }

    @Override
    protected Action deathAction() { return Action.DEATH; }

    @Override
    protected List<Cooldown> cooldowns() { return COOLDOWNS; }

    @Override
    protected Cooldown cooldownOf(Action attack) { return attack == Action.ATTACK_SWIPE ? SWIPE : HEAVY; }

    @Override
    protected String recoverySaveKey() { return "BeastRecoveryTicks"; }

    public int swipeCooldown() {
        return this.cooldown(SWIPE);
    }

    public int heavyCooldown() {
        return this.cooldown(HEAVY);
    }
    //endregion

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(2, new HuntGoal(this));
        this.goalSelector.addGoal(4, new RestGoal(this));
    }

    @Override
    protected void onBeginAction(Action next) {
        if (next.isAttack() && this.actionTarget != null) this.faceDirection(this.directionTo(this.actionTarget));
        if (next == Action.ROAR) this.playSound(this.roarSound(), 1.0F, this.roarPitch());
        this.ambientEndGameTime = next == Action.SIT
                ? this.level().getGameTime() + SIT_ACTION_TICKS : Long.MIN_VALUE;
    }

    @Override
    protected void onActionCleared() {
        this.ambientEndGameTime = Long.MIN_VALUE;
    }

    @Override
    protected boolean facesTargetInAction() {
        return this.action() == Action.ROAR;
    }

    //region action timing
    @Override
    protected void tickAction(Action current, long ticks) {
        if (current.isAmbient()) this.tickAmbientAction(current, ticks);
        else this.tickCombatAction(current, ticks);
    }

    private void tickCombatAction(Action current, long ticks) {
        switch (current) {
            case ROAR -> {
                if (ticks >= this.roarActionTicks()) this.finishAction();
            }
            case ATTACK_SWIPE -> {
                if (ticks >= this.swipeHitTick() && !this.hitSettled) {
                    this.hitSettled = true;
                    this.performSwipe();
                }
                if (ticks >= this.swipeActionTicks()) this.finishAction();
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
                if (ticks >= this.lieDownActionTicks()) {
                    this.switchAction(Action.LIE);
                    this.ambientEndGameTime = this.level().getGameTime() + LIE_ACTION_TICKS;
                }
            }
            case LIE -> {
                if (!this.isNightTime()) this.beginAction(Action.GET_UP, false);
                else if (this.level().getGameTime() >= this.ambientEndGameTime) this.switchAction(Action.SLEEP);
            }
            case SLEEP -> {
                if (!this.isNightTime()) this.beginAction(Action.GET_UP, false);
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

    private void performSwipe() {
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target) || this.distanceTo(target) > SWIPE_REACH
                || !this.hasLineOfSight(target)) return;
        this.applyAttack(target, this.swipeDamage(), this.swipeKnockback(), this.swipeUpward(),
                this.directionOrFacing(target));
    }

    private boolean isNightTime() {
        long dayTime = this.level().getDayTime() % Ticks.DAY;
        return dayTime >= NIGHT_START_TICK && dayTime <= NIGHT_END_TICK;
    }
    //endregion

    //region engagement
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (target != null && previous == null && !this.level().isClientSide() && !this.isDeadOrDying()
                && (this.action() == Action.IDLE || this.action().isAmbient())) {
            this.switchAction(Action.ROAR);
        }
    }

    @Override
    protected void reactToHurt(@Nullable LivingEntity attacker) {
        if (attacker != null && this.canAttackTarget(attacker)) this.lockTarget(attacker);
        Action current = this.action();
        if (!current.isAttack() && current != Action.DEATH && current != Action.ROAR
                && current != Action.HURT_LEFT && current != Action.HURT_RIGHT) {
            this.switchAction(this.directionalHurt(attacker));
        }
    }

    private Action directionalHurt(@Nullable Entity attacker) {
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
        this.rollAmbient();
    }

    private void rollAmbient() {
        if (!this.isIdle() || this.getTarget() != null || this.isDeadOrDying() || !this.onGround()) return;
        if (this.isNightTime()) {
            if (this.random.nextDouble() < NIGHT_LIE_DOWN_CHANCE) this.beginAction(Action.LIE_DOWN, false);
            return;
        }
        double roll = this.random.nextDouble();
        if (roll >= AMBIENT_REST_CHANCE) return;
        Action pick = this.pickDaytimeAmbient(roll / AMBIENT_REST_CHANCE);
        if (pick == null) return;
        this.beginAction(pick, false);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling().inflate(this.cullHorizontalInflate());
        return box.setMaxY(box.maxY + this.cullExtraHeight());
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(this.stepSound(), 0.15F, 1.0F);
    }

    @Override
    protected String animationPrefix() { return "animation."; }

    @Override
    protected RawAnimation actionAnimation(Action action) {
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
            case ATTACK_HEAVY -> clip.thenPlay(this.heavyAttackAnimation());
            case HURT_LEFT -> clip.thenPlay("animation.hurt_left");
            case HURT_RIGHT -> clip.thenPlay("animation.hurt_right");
            case DEATH -> clip.thenPlayAndHold("animation.death");
        };
    }

    private static final class HuntGoal extends Goal {

        private static final int SCAN_INTERVAL_TICKS = 10;
        private final BeastEntity beast;

        private HuntGoal(BeastEntity beast) {
            this.beast = beast;
        }

        @Override
        public boolean canUse() {
            Action action = this.beast.action();
            return this.beast.huntsActively() && this.beast.getTarget() == null
                    && action != Action.SLEEP && action != Action.GET_UP && action != Action.DEATH
                    && action != Action.ROAR && !action.isAttack()
                    && action != Action.HURT_LEFT && action != Action.HURT_RIGHT;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.beast.tickCount % SCAN_INTERVAL_TICKS != 0) return;
            double range = this.beast.getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB box = this.beast.getBoundingBox().inflate(range);
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : this.beast.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> (entity instanceof Player || entity.getType().is(ModEntityTypeTags.PREDATOR_PREY))
                            && this.beast.canAttackTarget(entity) && this.beast.hasLineOfSight(entity))) {
                double distance = this.beast.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            if (best != null) this.beast.setTarget(best);
        }
    }

    private static final class RestGoal extends Goal {

        private final BeastEntity beast;

        private RestGoal(BeastEntity beast) {
            this.beast = beast;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.beast.action().isAmbient() && this.beast.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            this.beast.getNavigation().stop();
        }
    }

    /** The synchronized poses; {@link #isAmbient} marks the rest poses that combat engagement interrupts. */
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
        public boolean loops() { return this.loop; }

        @Override
        public boolean isAttack() { return this.attack; }

        @Override
        public boolean isAmbient() { return this.ambient; }
    }
}
