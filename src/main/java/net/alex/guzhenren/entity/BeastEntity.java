package net.alex.guzhenren.entity;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
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
 * The big beasts (bears, tigers): the {@link ActionEntity} state machine with a swipe and a heavy attack, a
 * first-lock roar, directional hurt poses and the night-sleep / daytime-rest ambient chain.
 *
 * <p>Subclasses supply the timing constants: {@link #getSwipeHitTick} is the tick at which the swipe's hit frame
 * lands; {@link #getSwipeActionTicks} the tick at which the swipe ends; {@link #getRoarActionTicks} the length of the
 * first-lock roar; {@link #getLieDownActionTicks} the lie-down transition into lie/sleep; {@link #getUpActionTicks}
 * the get-up transition back to idle. The attack: {@link #getSwipeDamage} and {@link #getHeavyDamage} are the flat
 * damage before armor; {@link #getSwipeKnockback} and {@link #getSwipeUpward} the swipe's knockback;
 * {@link #tickHeavyAttack} advances the heavy attack, hit frames and movement physics included.
 * {@link #isActiveHunter} says whether the beast seeks targets on its own or only retaliates;
 * {@link #pickDaytimeAmbient} picks the daytime ambient action, with relative weights;
 * {@link #getCullExtraHeight} and {@link #getCullHorizontalInflate} extend the render culling box so rearing poses
 * and tail sweeps are not clipped, recomputed only when the body's bounding box is replaced;
 * {@link #getHeavyAttackAnimation} names the heavy attack's animation. {@link #canSwipe} is the swipe's shared
 * precondition: in reach, off cooldown and in sight.
 *
 * <p>{@link HuntGoal} is registered unconditionally: {@code registerGoals} runs inside the {@code Mob}
 * constructor, before subclass fields like the bear species are assigned, so the temperament check happens
 * at tick time. A beast that becomes engaged in an ambient pose is snapped straight out of it without a
 * get-up transition.
 *
 * <p>In {@link #setTarget}, the first lock of an engagement is announced by the roar; it never interrupts a
 * running action. In {@link #reactToHurt}, a running hurt pose is never restarted: it outlasts vanilla's
 * 10-tick damage window, so restarting it would let steady hits stun-lock the beast out of every attack.
 * {@link #pickDirectionalHurt} picks the hurt pose from the attacker's bearing: an attacker on this beast's left
 * side knocks the head to the beast's right, playing {@code hurt_right}, and vice versa.
 *
 * <p>{@link #rollAmbient} picks and starts an ambient action after a completed wander; nothing happens on a
 * miss. In {@link HuntGoal#canUse}, any awake, non-combat pose notices prey -- including sitting, lying,
 * rolling and scratching; only actual sleep, the get-up transition and ongoing combat skip hunting. A
 * {@link StandStillGoal} holds navigation still while the beast sits, lies, sleeps, rolls or scratches.
 * {@link HuntGoal} scans every ten ticks, staggered by entity id so neighboring beasts do not scan together.
 *
 * <p>Past 300 lines on purpose: the fourteen poses, their animations, the ambient chain and the beast-only
 * hunt goal belong to this layer.
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

    private static final Cooldown SWIPE = new Cooldown(SWIPE_COOLDOWN_TICKS, "BeastSwipeCooldown");
    private static final Cooldown HEAVY = new Cooldown(HEAVY_COOLDOWN_TICKS, "BeastHeavyCooldown");
    private static final List<Cooldown> COOLDOWNS = List.of(SWIPE, HEAVY);

    private long ambientEndGameTime = Long.MIN_VALUE;
    private @Nullable AABB cullSource;
    private AABB cullBox;

    protected BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
    }

    //region subclass hooks
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

    //region action set
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

    //region action timing
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
                && (this.getAction() == Action.IDLE || this.getAction().isAmbient())) {
            this.switchAction(Action.ROAR);
        }
    }

    @Override
    protected void reactToHurt(@Nullable LivingEntity attacker) {
        if (attacker != null && this.canAttackTarget(attacker)) this.lockTarget(attacker);
        Action current = this.getAction();
        if (!current.isAttack() && current != Action.DEATH && current != Action.ROAR
                && current != Action.HURT_LEFT && current != Action.HURT_RIGHT) {
            this.switchAction(this.pickDirectionalHurt(attacker));
        }
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
        this.rollAmbient();
    }

    private void rollAmbient() {
        if (!this.isIdle() || this.getTarget() != null || this.isDeadOrDying() || !this.onGround()) return;
        if (this.isNightTime()) {
            if (this.random.nextDouble() < NIGHT_LIE_DOWN_CHANCE) this.tryBeginAction(Action.LIE_DOWN, false);
            return;
        }
        double roll = this.random.nextDouble();
        if (roll >= AMBIENT_REST_CHANCE) return;
        Action pick = this.pickDaytimeAmbient(roll / AMBIENT_REST_CHANCE);
        if (pick == null) return;
        this.tryBeginAction(pick, false);
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

        private HuntGoal(BeastEntity beast) {
            this.beast = beast;
        }

        @Override
        public boolean canUse() {
            Action action = this.beast.getAction();
            return this.beast.isActiveHunter() && this.beast.getTarget() == null
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
            if ((this.beast.tickCount + this.beast.getId()) % SCAN_INTERVAL_TICKS != 0) return;
            double range = this.beast.getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB box = this.beast.getBoundingBox().inflate(range);
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : this.beast.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> (entity instanceof Player || entity.getType().is(ModEntityTypeTags.PREDATOR_PREY))
                            && this.beast.canAttackTarget(entity)
                            && this.beast.getSensing().hasLineOfSight(entity))) {
                double distance = this.beast.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            if (best != null) this.beast.setTarget(best);
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
        public boolean isLooping() { return this.loop; }

        @Override
        public boolean isAttack() { return this.attack; }

        @Override
        public boolean isAmbient() { return this.ambient; }
    }
}
