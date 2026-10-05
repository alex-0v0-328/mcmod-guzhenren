package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.AttackContributor;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The timed buff of the Brute Force Longhorn Beetle Gu [蛮力天牛蛊]: added attack damage for thirty
 * seconds, followed by a twenty-second weakness aftermath.
 *
 * <p>Timed effects own their truth on vanilla's timer. The attack bonus goes through {@link
 * net.alex.guzhenren.effect.AttackContributor}; the aftermath is applied on the effect's last
 * tick ({@code duration == 1}), because a {@link net.minecraft.world.effect.MobEffect} has no expiry
 * hook. Changing how that last tick is recognized is how the penalty quietly stops happening.
 *
 * <p>⚠ The aftermath is a punishment — milk skipping it is the intended design, NOT a gap.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.AttackContributor
 * @since 1.0.0
 */

public class BruteForceLonghornBeetleGuEffect extends MobEffect implements AttackContributor {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;
    public static final int AFTERMATH_TICKS = 20 * Ticks.SECOND;
    public static final double ATTACK_BONUS = 8.0D;
    private static final int LAST_TICK = 1;

    public BruteForceLonghornBeetleGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double attackBonus(int amplifier) { return ATTACK_BONUS; }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration == LAST_TICK; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(ModEffects.instance(MobEffects.WEAKNESS, AFTERMATH_TICKS));
        return true;
    }
}
