package net.alex.guzhenren.entity.beast;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 野猪：{@link ChargerEntity}，动画名无前缀、猪音效，冲撞 {@value #CHARGE_DAMAGE}、掀击 {@value #TOSS_DAMAGE}。
 */

public final class WildBoarEntity extends ChargerEntity {

    public static final int HURT_ACTION_TICKS = 7;
    private static final float CHARGE_DAMAGE = 8.0F;
    private static final float TOSS_DAMAGE = 6.0F;
    private static final List<Cooldown> COOLDOWNS = createCooldowns("WildBoar");

    public WildBoarEntity(EntityType<? extends WildBoarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    @Override
    protected List<Cooldown> getCooldowns() { return COOLDOWNS; }

    @Override
    protected String getRecoverySaveKey() { return "WildBoarRecoveryTicks"; }

    @Override
    protected float getChargeDamage() { return CHARGE_DAMAGE; }

    @Override
    protected float getTossDamage() { return TOSS_DAMAGE; }

    @Override
    protected int getHurtActionTicks() { return HURT_ACTION_TICKS; }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    protected String getAnimationPrefix() { return ""; }
}
