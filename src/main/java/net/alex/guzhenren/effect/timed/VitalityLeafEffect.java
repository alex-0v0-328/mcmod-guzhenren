package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The timed healing of the Vitality Leaf Gu [生机叶蛊]: heals one HP every half-second for 64 pulses
 * on its own vanilla timer.
 *
 * <p>Timed effects own their truth on vanilla's {@link net.minecraft.world.effect.MobEffect} timer —
 * unlike pool effects, they are not rebuilt every heartbeat, so milk does cure them. Re-using the
 * Gu while this effect runs is a refusal ({@code vitality_active}).
 *
 * <p>A timed buff alters nothing permanently.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class VitalityLeafEffect extends PeriodicEffect {

    public static final int HEAL_INTERVAL_TICKS = Ticks.HALF_SECOND;
    public static final int DURATION_TICKS = 64 * HEAL_INTERVAL_TICKS;

    public VitalityLeafEffect(MobEffectCategory category, int color) {
        super(category, color, HEAL_INTERVAL_TICKS);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.heal(1.0F);
        return true;
    }
}
