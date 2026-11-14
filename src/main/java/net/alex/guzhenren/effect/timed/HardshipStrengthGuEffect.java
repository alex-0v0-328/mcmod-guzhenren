package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 苦力蛊[Hardship Strength Gu]效果，仅苦力状态图标：状态即限时流派效果 {@code ModPathEffectTypes.HARDSHIP_STRENGTH}，
 * 由其投影本图标与窗口倒计时；加成归 {@code HardshipStrength}。
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
