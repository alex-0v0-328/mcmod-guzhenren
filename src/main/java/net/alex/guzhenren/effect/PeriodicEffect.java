package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * An effect that pulses once every {@code intervalTicks} of its remaining duration; the subclass's
 * {@code applyEffectTick} is the pulse.
 *
 * <p>⚠ The pulse must {@code return true} -- returning false lets vanilla remove the effect on the spot.
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

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % intervalTicks == 0;
    }
}
