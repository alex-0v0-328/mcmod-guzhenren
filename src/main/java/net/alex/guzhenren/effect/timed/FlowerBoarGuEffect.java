package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.AftermathEffect;
import net.alex.guzhenren.gameplay.attribute.AttackContributor;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Flower Boar Gu [花豕蛊]: one boar's strength [一猪之力] of added attack damage for sixty
 * seconds, followed by the weakness aftermath.
 *
 * <p>The path effect is the buff's truth and this vanilla effect its icon and timer. The attack bonus goes through
 * {@link AttackContributor} so the attack total stays one number; the aftermath comes from {@link AftermathEffect},
 * the length is this class's own.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackContributor
 * @since 1.0.0
 */

public class FlowerBoarGuEffect extends AftermathEffect implements AttackContributor {

    public static final int DURATION_TICKS = 60 * Ticks.SECOND;
    public static final double ATTACK_BONUS = 3.0D;

    public FlowerBoarGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double attackBonus(int amplifier) { return ATTACK_BONUS; }
}
