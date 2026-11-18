package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 按剩余持续时间每隔 {@code intervalTicks} 触发一次脉冲的 {@link MobEffect} 基类。本类统一应答原版的逐刻回调并通过
 * {@link #isPulseTick} 控制节奏（脉冲始终返回 {@code true} 而不提前终止效果），子类只需覆写 {@link #onPulse} 实现单次脉
 * 冲行为。
 */

public abstract class PeriodicEffect extends MobEffect {

    private final int intervalTicks;

    protected PeriodicEffect(MobEffectCategory category, int color, int intervalTicks) {
        super(category, color);
        this.intervalTicks = intervalTicks;
    }

    public boolean isPulseTick(int duration) { return duration % intervalTicks == 0; }

    protected abstract void onPulse(@NotNull LivingEntity entity, int amplifier);

    @Override
    public final boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return isPulseTick(duration); }

    @Override
    public final boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        onPulse(entity, amplifier);
        return true;
    }
}
