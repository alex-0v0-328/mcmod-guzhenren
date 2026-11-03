package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Flower Boar Gu [花豕蛊]: one boar's strength [一猪之力] of strength points for thirty
 * seconds, with no aftermath.
 *
 * <p>The path effect is the buff's truth and this vanilla effect its icon and timer. The points go through
 * {@link BeastStrengthBuff} into the one strength settlement, so the attack total stays one number and the
 * carrying capacity applies.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrengthBuff
 * @since 1.0.0
 */

public class FlowerBoarGuEffect extends MobEffect implements BeastStrengthBuff {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;
    public static final int POINTS = BeastStrength.WHITE_BOAR.getPoints();

    public FlowerBoarGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public BeastStrengthFamily getBeastFamily() { return BeastStrengthFamily.BOAR; }

    @Override
    public int getPoints() { return POINTS; }
}
