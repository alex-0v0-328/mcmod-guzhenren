package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * An effect that pulses once every {@code intervalTicks} of its remaining duration; the subclass's
 * {@link #onPulse} is the pulse.
 *
 * <p>⚠ This class answers vanilla's per-tick callbacks ({@link #isPulseTick} decides the cadence, the pulse never
 * ends the effect early), so subclasses override {@link #onPulse} only.
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

    /** Whether the effect pulses on the tick with {@code duration} remaining. */
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
