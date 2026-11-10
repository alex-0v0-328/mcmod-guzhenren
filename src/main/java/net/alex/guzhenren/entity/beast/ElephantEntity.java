package net.alex.guzhenren.entity.beast;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * The elephant and the white elephant, two entity types that differ only in texture (tiger precedent): a
 * {@link ChargerEntity} on the {@code elephant} model, which is authored at half size and drawn doubled, so the
 * body is about 3.1 by 3.75 by 4.7 blocks around a {@code 2.5 x 3.7} hitbox. It retaliates and never hunts.
 *
 * <p>Beyond {@value #CLOSE_REACH} blocks it charges like every charger. Within them it takes the first attack
 * off cooldown, in this order: the stomp (every {@value #STOMP_COOLDOWN_TICKS} ticks, landing on tick
 * {@value #STOMP_HIT_TICK} on everything attackable within {@value #STOMP_RADIUS} blocks, other elephants
 * excepted so a herd never turns on itself), the tusk toss (every {@value #TOSS_COOLDOWN_TICKS} ticks) and the
 * trunk sweep (every {@value #TRUNK_COOLDOWN_TICKS} ticks, landing on tick {@value #TRUNK_HIT_TICK}, one target,
 * knocked flat away).
 *
 * <p>{@link #MOVEMENT_SPEED} and {@link #PURSUIT_SPEED} follow the clips' strides: an AI-driven mob covers
 * {@code 43.17 x speed^2} blocks a second, so the walk clip's 1.27 and the run clip's 3.58 blocks a second give
 * the attribute and the pursuit multiplier. The culling box is widened because the head, trunk and tail reach
 * about 1.1 blocks past the hitbox and the stomp rears above it. No elephant sound exists in vanilla; the camel's,
 * pitched down, stand in.
 */

public final class ElephantEntity extends ChargerEntity {

    public static final double MAX_HEALTH = 120.0D;
    public static final double ARMOR = 4.0D;
    public static final double KNOCKBACK_RESISTANCE = 1.0D;
    public static final double MOVEMENT_SPEED = 0.1715D;
    public static final double PURSUIT_SPEED = 1.68D;
    public static final double STEP_HEIGHT = 1.0D;
    public static final float CHARGE_DAMAGE = 16.0F;
    public static final float TOSS_DAMAGE = 10.0F;
    public static final float STOMP_DAMAGE = 18.0F;
    public static final float TRUNK_DAMAGE = 8.0F;
    public static final double CLOSE_REACH = 3.0D;
    public static final double STOMP_RADIUS = 3.0D;
    public static final int TOSS_COOLDOWN_TICKS = 60;
    public static final int STOMP_COOLDOWN_TICKS = 200;
    public static final int TRUNK_COOLDOWN_TICKS = 40;
    public static final int STOMP_HIT_TICK = 14;
    public static final int STOMP_ACTION_END_TICKS = 24;
    public static final int TRUNK_HIT_TICK = 9;
    public static final int TRUNK_ACTION_END_TICKS = 20;
    public static final int HURT_ACTION_TICKS = 8;

    private static final double STOMP_KNOCKBACK = 0.8D;
    private static final double STOMP_UPWARD = 0.4D;
    private static final double TRUNK_KNOCKBACK = 1.2D;
    private static final double CULL_HORIZONTAL_INFLATE = 1.2D;
    private static final double CULL_EXTRA_HEIGHT = 1.0D;
    private static final float VOICE_PITCH = 0.6F;
    private static final String ANIMATION_PREFIX = "animation.";
    private static final Cooldown CHARGE = new Cooldown(CHARGE_COOLDOWN_TICKS, "ElephantChargeCooldown");
    private static final Cooldown TOSS = new Cooldown(TOSS_COOLDOWN_TICKS, "ElephantTossCooldown");
    private static final Cooldown STOMP = new Cooldown(STOMP_COOLDOWN_TICKS, "ElephantStompCooldown");
    private static final Cooldown TRUNK = new Cooldown(TRUNK_COOLDOWN_TICKS, "ElephantTrunkCooldown");
    private static final List<Cooldown> COOLDOWNS = List.of(CHARGE, TOSS, STOMP, TRUNK);

    public ElephantEntity(EntityType<? extends ElephantEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.STEP_HEIGHT, STEP_HEIGHT)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    //region charger hooks [冲撞者钩子]
    @Override
    protected List<Cooldown> getCooldowns() { return COOLDOWNS; }

    @Override
    protected Cooldown getCooldownOf(Action attack) {
        return switch (attack) {
            case ATTACK_STOMP -> STOMP;
            case ATTACK_TRUNK -> TRUNK;
            default -> super.getCooldownOf(attack);
        };
    }

    @Override
    protected String getRecoverySaveKey() { return "ElephantRecoveryTicks"; }

    @Override
    protected double getPursuitSpeed() { return PURSUIT_SPEED; }

    @Override
    protected float getChargeDamage() { return CHARGE_DAMAGE; }

    @Override
    protected float getTossDamage() { return TOSS_DAMAGE; }

    @Override
    protected double getTossReach() { return CLOSE_REACH; }

    @Override
    protected int getHurtActionTicks() { return HURT_ACTION_TICKS; }

    @Override
    protected String getAnimationPrefix() { return ANIMATION_PREFIX; }

    public int getStompCooldown() {
        return this.getCooldown(STOMP);
    }
    //endregion

    //region attacks [攻击]
    @Override
    protected void chooseAttack(LivingEntity target) {
        if (this.distanceTo(target) <= CLOSE_REACH && this.getSensing().hasLineOfSight(target)) {
            if (this.getCooldown(STOMP) == 0) {
                this.tryStartAction(Action.ATTACK_STOMP);
                return;
            }
            if (this.getCooldown(TOSS) > 0 && this.getCooldown(TRUNK) == 0) {
                this.tryStartAction(Action.ATTACK_TRUNK);
                return;
            }
        }
        super.chooseAttack(target);
    }

    @Override
    protected void tickSpeciesAttack(Action current, long ticks) {
        if (current == Action.ATTACK_STOMP) {
            if (ticks >= STOMP_HIT_TICK && !this.hitSettled) this.stomp();
            if (ticks >= STOMP_ACTION_END_TICKS) this.finishAction();
        } else {
            if (ticks >= TRUNK_HIT_TICK && !this.hitSettled) {
                this.strikeTarget(CLOSE_REACH, TRUNK_DAMAGE, TRUNK_KNOCKBACK, 0.0D);
            }
            if (ticks >= TRUNK_ACTION_END_TICKS) this.finishAction();
        }
    }

    private void stomp() {
        this.hitSettled = true;
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(STOMP_RADIUS), victim -> !(victim instanceof ElephantEntity)
                        && this.canAttackTarget(victim) && this.distanceTo(victim) <= STOMP_RADIUS
                        && this.getSensing().hasLineOfSight(victim));
        for (LivingEntity victim : victims) {
            this.applyAttack(victim, STOMP_DAMAGE, STOMP_KNOCKBACK, STOMP_UPWARD, this.getDirectionOrFacing(victim));
        }
    }
    //endregion

    //region presentation [表现]
    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling().inflate(CULL_HORIZONTAL_INFLATE, 0.0D, CULL_HORIZONTAL_INFLATE);
        return box.setMaxY(box.maxY + CULL_EXTRA_HEIGHT);
    }

    @Override
    public float getVoicePitch() { return VOICE_PITCH; }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.CAMEL_STEP, 0.3F, VOICE_PITCH);
    }
    //endregion
}
