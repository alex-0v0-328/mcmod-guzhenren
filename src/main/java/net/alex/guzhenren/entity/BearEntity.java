package net.alex.guzhenren.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A bear, one entity type per {@link BearSpecies}.
 *
 * <p>Combat alternates the four-legged swipe (hit frame at tick 9) with the rarer rear-up slam
 * (hit frame at tick 17, about one in three melee attacks); {@link #tickHeavyAttack} is stationary,
 * with the hit landing when the paw comes down. Daytime rests cycle sit, roll and back-scratch --
 * {@link #pickDaytimeAmbient} sits half the time, splitting the rest between roll and back-scratch --
 * nights lie down and sleep. Vanilla polar bear sounds stand in; the first-lock roar reuses the polar
 * bear warning sound.
 *
 * <p>{@link #huntsActively} is null-safe: {@code registerGoals} evaluates goals during the
 * {@code Mob} constructor, before species is set.
 */

public final class BearEntity extends BeastEntity {

    public static final int SWIPE_HIT_TICK = 9;
    public static final int SWIPE_END_TICKS = 20;
    public static final int REAR_HIT_TICK = 17;
    public static final int REAR_END_TICKS = 36;
    public static final int ROAR_TICKS = 52;
    public static final int LIE_DOWN_TICKS = 28;
    public static final int GET_UP_TICKS = 24;
    public static final int DEATH_REMOVE_TICK = 36;
    public static final double REAR_REACH = 3.0D;
    private static final double REAR_CHANCE = 1.0D / 3.0D;
    private static final double FOLLOW_RANGE_BLOCKS = 16.0D;
    private static final double MOVEMENT_SPEED = 0.25D;

    private final BearSpecies species;

    public BearEntity(EntityType<? extends BearEntity> type, Level level, BearSpecies species) {
        super(type, level);
        this.species = species;
    }

    public static AttributeSupplier.Builder createAttributes(BearSpecies species) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, species.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    public BearSpecies species() {
        return this.species;
    }

    @Override
    protected int swipeHitTick() { return SWIPE_HIT_TICK; }

    @Override
    protected int swipeActionTicks() { return SWIPE_END_TICKS; }

    @Override
    protected int roarActionTicks() { return ROAR_TICKS; }

    @Override
    protected int lieDownActionTicks() { return LIE_DOWN_TICKS; }

    @Override
    protected int getUpActionTicks() { return GET_UP_TICKS; }

    @Override
    protected int deathRemoveTick() { return DEATH_REMOVE_TICK; }

    @Override
    protected float swipeDamage() { return this.species.swipeDamage(); }

    @Override
    protected float heavyDamage() { return this.species.rearDamage(); }

    @Override
    protected double swipeKnockback() { return 1.0D; }

    @Override
    protected double swipeUpward() { return 0.0D; }

    @Override
    protected boolean huntsActively() {
        return this.species != null && this.species.hostile();
    }

    @Override
    protected double pursuitSpeed() { return 1.4D; }

    @Override
    protected double cullExtraHeight() { return 1.3D; }

    @Override
    protected double cullHorizontalInflate() { return 0.3D; }

    @Override
    protected void chooseAttack(LivingEntity target) {
        if (this.distanceTo(target) > SWIPE_REACH || this.swipeCooldown() > 0
                || !this.hasLineOfSight(target)) return;
        if (this.heavyCooldown() == 0 && this.random.nextDouble() < REAR_CHANCE) {
            this.startAction(Action.ATTACK_HEAVY);
        } else {
            this.startAction(Action.ATTACK_SWIPE);
        }
    }

    @Override
    protected void tickHeavyAttack(long ticks) {
        if (ticks >= REAR_HIT_TICK && !this.hitSettled) {
            this.hitSettled = true;
            LivingEntity target = this.actionTarget;
            if (target != null && this.canAttackTarget(target) && this.distanceTo(target) <= REAR_REACH
                    && this.hasLineOfSight(target)) {
                this.applyAttack(target, this.heavyDamage(), 0.5D, 0.0D, this.directionOrFacing(target));
            }
        }
        if (ticks >= REAR_END_TICKS) this.finishAction();
    }

    @Override
    protected Action pickDaytimeAmbient(double roll) {
        if (roll < 0.5D) return Action.SIT;
        return roll < 0.75D ? Action.ROLL : Action.BACK_SCRATCH;
    }

    @Override
    protected String heavyAttackAnimation() { return "animation.attack_rear"; }

    @Override
    protected SoundEvent roarSound() { return SoundEvents.POLAR_BEAR_WARNING; }

    @Override
    protected float roarPitch() { return 1.0F; }

    @Override
    protected SoundEvent stepSound() { return SoundEvents.POLAR_BEAR_STEP; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.POLAR_BEAR_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.@NotNull DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.POLAR_BEAR_DEATH; }
}
