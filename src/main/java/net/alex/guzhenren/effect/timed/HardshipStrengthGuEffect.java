package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 苦力蛊[Hardship Strength Gu]的纯图标标记效果，仅用于显示倒计时与状态图标；实际状态真值由限时流派效果
 * {@code ModPathEffectTypes.HARDSHIP_STRENGTH} 持有，具体数值加成由
 * {@link net.alex.guzhenren.gameplay.path.strength.HardshipStrength} 结算。
 */

public final class HardshipStrengthGuEffect extends MobEffect {

    public HardshipStrengthGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
