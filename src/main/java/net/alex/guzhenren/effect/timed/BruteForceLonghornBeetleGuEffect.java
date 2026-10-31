package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.attribute.AttackContributor;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Brute Force Longhorn Beetle Gu [蛮力天牛蛊]: one ox's strength [一牛之力] of added attack
 * damage for forty-five seconds, with no aftermath.
 *
 * <p>The path effect is the buff's truth and this vanilla effect its icon and timer. The attack bonus goes through
 * {@link AttackContributor} so the attack total stays one number. The ox lasts longer than the boar; that length is
 * its trait, not a stronger bonus.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackContributor
 * @see BeastStrengthBuff
 * @since 1.0.0
 */

public class BruteForceLonghornBeetleGuEffect extends MobEffect implements AttackContributor, BeastStrengthBuff {

    public static final int DURATION_TICKS = 45 * Ticks.SECOND;
    public static final double ATTACK_BONUS = 5.0D;

    public BruteForceLonghornBeetleGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double attackBonus(int amplifier) { return ATTACK_BONUS; }

    @Override
    public BeastStrengthFamily getBeastFamily() { return BeastStrengthFamily.OX; }
}
