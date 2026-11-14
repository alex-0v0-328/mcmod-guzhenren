package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 花豕蛊[Flower Boar Gu]限时增益：一猪之力力量点[Strength Points]，30 秒，结束无余波；真值为流派效果，本效果仅图标与计
 * 时；点数经 {@link BeastStrengthBuff} 入唯一力量结算，攻击总值仍为一数，承受力照算。
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
}
