package net.alex.guzhenren.effect.pool;

import net.alex.guzhenren.gameplay.attribute.AttackContributor;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Strength Qi [力气] effect — a pool projection of the 力气 held in {@link
 * PathQiData}, which adds attack damage while held.
 *
 * <p>Pool effects are rebuilt every heartbeat by {@code PathQiService.syncEffects}, so milk cannot cure
 * them. It contributes via {@link net.alex.guzhenren.gameplay.attribute.AttackContributor}, not an attribute
 * modifier, so the body panel and a dealt hit stay one number.
 *
 * <p>⚠ The {@code ATTACK_BONUS} ladder {0.25, 1, 4, 16, 64} must be exactly representable as a
 * {@code double} — power-of-two denominators only, no float dust.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.attribute.AttackContributor
 * @since 1.0.0
 */

public class StrengthQiEffect extends MobEffect implements AttackContributor {

    private static final double[] ATTACK_BONUS = { 0.25, 1.0, 4.0, 16.0, 64.0 };

    public StrengthQiEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double attackBonus(int amplifier) {
        return ATTACK_BONUS[Math.clamp(amplifier, 0, ATTACK_BONUS.length - 1)];
    }
}
