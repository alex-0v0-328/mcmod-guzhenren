package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Hardship Strength Gu [苦力蛊] effect: the icon of the Hardship state, and nothing more. The state is the permanent
 * path effect {@code ModPathEffectTypes.HARDSHIP_STRENGTH}, which projects this icon with no countdown for as long as
 * it runs; what it lends is {@code HardshipStrength}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.HardshipStrength
 * @since 1.0.0
 */

public final class HardshipStrengthGuEffect extends MobEffect {

    public HardshipStrengthGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
