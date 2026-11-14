package net.alex.guzhenren.entity.beast;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 熊，每个 {@link BearSpecies} 一个实体类型。
 * <ul>
 *   <li>四足挥击（第 9 tick 命中）与较少的站立拍击（第 17 tick，约占近战三分之一）交替；拍击原地结算，掌落即中。</li>
 *   <li>白天坐、翻滚、蹭背轮换（坐占半，余二均分），夜里趴下入睡；借北极熊音效，首锁吼叫用其警告音。</li>
 *   <li>{@link #isActiveHunter} 容忍空值：{@code registerGoals} 在 {@code Mob} 构造器内、种类赋值前评估 AI 目标。</li>
 * </ul>
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
                .add(Attributes.MAX_HEALTH, species.getMaxHealth())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    @Override
    protected int getSwipeHitTick() { return SWIPE_HIT_TICK; }

    @Override
    protected int getSwipeActionTicks() { return SWIPE_END_TICKS; }

    @Override
    protected int getRoarActionTicks() { return ROAR_TICKS; }

    @Override
    protected int getLieDownActionTicks() { return LIE_DOWN_TICKS; }

    @Override
    protected int getUpActionTicks() { return GET_UP_TICKS; }

    @Override
    protected int getDeathRemoveTick() { return DEATH_REMOVE_TICK; }

    @Override
    protected float getSwipeDamage() { return this.species.getSwipeDamage(); }

    @Override
    protected float getHeavyDamage() { return this.species.getRearDamage(); }

    @Override
    protected double getSwipeKnockback() { return 1.0D; }

    @Override
    protected double getSwipeUpward() { return 0.0D; }

    @Override
    protected boolean isActiveHunter() {
        return this.species != null && this.species.isHostile();
    }

    @Override
    protected double getPursuitSpeed() { return 1.4D; }

    @Override
    protected double getCullExtraHeight() { return 1.3D; }

    @Override
    protected double getCullHorizontalInflate() { return 0.3D; }

    @Override
    protected void chooseAttack(LivingEntity target) {
        if (!this.canSwipe(target)) return;
        if (this.getHeavyCooldown() == 0 && this.random.nextDouble() < REAR_CHANCE) {
            this.tryStartAction(Action.ATTACK_HEAVY);
        } else {
            this.tryStartAction(Action.ATTACK_SWIPE);
        }
    }

    @Override
    protected void tickHeavyAttack(long ticks) {
        if (ticks >= REAR_HIT_TICK && !this.hitSettled) {
            this.strikeTarget(REAR_REACH, this.getHeavyDamage(), 0.5D, 0.0D);
        }
        if (ticks >= REAR_END_TICKS) this.finishAction();
    }

    @Override
    protected Action pickDaytimeAmbient(double roll) {
        if (roll < 0.5D) return Action.SIT;
        return roll < 0.75D ? Action.ROLL : Action.BACK_SCRATCH;
    }

    @Override
    protected String getHeavyAttackAnimation() { return "animation.attack_rear"; }

    @Override
    protected SoundEvent getRoarSound() { return SoundEvents.POLAR_BEAR_WARNING; }

    @Override
    protected float getRoarPitch() { return 1.0F; }

    @Override
    protected SoundEvent getStepSound() { return SoundEvents.POLAR_BEAR_STEP; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.POLAR_BEAR_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.POLAR_BEAR_DEATH; }
}
