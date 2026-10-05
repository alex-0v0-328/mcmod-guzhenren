package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.effect.AftermathEffect;
import net.alex.guzhenren.gameplay.attribute.AttackContributor;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Brute Force Longhorn Beetle Gu [蛮力天牛蛊]: added attack damage for thirty
 * seconds, followed by a twenty-second weakness aftermath.
 *
 * <p>Timed effects own their truth on vanilla's timer. The attack bonus goes through {@link
 * AttackContributor}; the duration and the aftermath come from {@link AftermathEffect}.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackContributor
 * @since 1.0.0
 */

public class BruteForceLonghornBeetleGuEffect extends AftermathEffect implements AttackContributor {

    public static final double ATTACK_BONUS = 8.0D;

    public BruteForceLonghornBeetleGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double attackBonus(int amplifier) { return ATTACK_BONUS; }
}
