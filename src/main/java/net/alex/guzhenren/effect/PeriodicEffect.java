package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 按剩余时长每 {@code intervalTicks} 脉冲一次的效果。⚠ 原版逐 tick 回调由本类应答（{@link #isPulseTick} 定节奏，脉冲不
 * 提前结束效果），子类只覆写脉冲 {@link #onPulse}。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
