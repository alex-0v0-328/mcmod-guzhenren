package net.alex.guzhenren.effect;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * A thirty-second beast buff [兽力] that leaves a twenty-second weakness aftermath. Subclasses add the
 * buff itself (attack through {@code AttackContributor}, or an attribute modifier); this class owns the
 * timing and the penalty.
 *
 * <p>The aftermath is applied on the effect's last tick ({@code duration == 1}), because a
 * {@link MobEffect} has no expiry hook. Changing how that last tick is recognized is how the penalty
 * quietly stops happening. ⚠ The aftermath is a punishment -- milk or {@code /effect clear} skipping it
 * is the intended design, NOT a gap.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class AftermathEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;
    public static final int AFTERMATH_TICKS = 20 * Ticks.SECOND;
    private static final int LAST_TICK = 1;

    protected AftermathEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration == LAST_TICK; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(ModEffects.instance(MobEffects.WEAKNESS, AFTERMATH_TICKS));
        return true;
    }
}
