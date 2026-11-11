package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Brute Force Longhorn Beetle Gu [蛮力天牛蛊]: one ox's strength [一牛之力] of strength
 * points for forty-five seconds, with no aftermath.
 *
 * <p>The path effect is the buff's truth and this vanilla effect its icon and timer. The points go through
 * {@link BeastStrengthBuff} into the one strength settlement, so the attack total stays one number and the
 * carrying capacity applies. An ox is worth a bear's points; the ox lasts longer than the boar, and that length
 * is its trait.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrengthBuff
 * @since 1.0.0
 */

public class BruteForceLonghornBeetleGuEffect extends MobEffect implements BeastStrengthBuff {

    public static final int DURATION_TICKS = 45 * Ticks.SECOND;
    public static final int POINTS = BeastStrength.BEAR.getPoints();

    public BruteForceLonghornBeetleGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public BeastStrengthFamily getBeastFamily() { return BeastStrengthFamily.OX; }
}
