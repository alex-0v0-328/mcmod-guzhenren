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
 * 牛实体：每个 {@link CattleSpecies} 对应一个独立实体类型，继承 {@link ChargerEntity}。
 * <ul>
 *   <li>共用 {@code buffalo} 模型、{@code animation.} 前缀片段与原版牛音效，冲撞与角挑伤害取自 {@link CattleSpecies}；
 *       移速按动画步幅匹配（AI 生物地面移速约为 {@code 43.17 * speed^2} 格/秒，移速 {@value #MOVEMENT_SPEED} 与追击倍率
 *       1.4 分别对应行走 1.73 格/秒、奔跑 3.4 格/秒）。</li>
 *   <li>⚠ {@code species} 字段仅在 tick 阶段读取，避免 {@code Mob} 构造器执行 {@code registerGoals} 时访问未初始化的子
 *       类字段。</li>
 * </ul>
 */

public final class CattleEntity extends ChargerEntity {

    public static final int HURT_ACTION_TICKS = 8;
    public static final double MAX_HEALTH = 20.0D;
    public static final double MOVEMENT_SPEED = 0.2D;
    private static final String ANIMATION_PREFIX = "animation.";
    private static final List<Cooldown> COOLDOWNS = createCooldowns("Cattle");

    private final CattleSpecies species;

    public CattleEntity(EntityType<? extends CattleEntity> type, Level level, CattleSpecies species) {
        super(type, level);
        this.species = species;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    public CattleSpecies getSpecies() { return this.species; }

    @Override
    protected List<Cooldown> getCooldowns() { return COOLDOWNS; }

    @Override
    protected String getRecoverySaveKey() { return "CattleRecoveryTicks"; }

    @Override
    protected float getChargeDamage() { return this.species.getChargeDamage(); }

    @Override
    protected float getTossDamage() { return this.species.getTossDamage(); }

    @Override
    protected int getHurtActionTicks() { return HURT_ACTION_TICKS; }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.COW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.COW_STEP, 0.15F, 1.0F);
    }

    @Override
    protected String getAnimationPrefix() { return ANIMATION_PREFIX; }
}
