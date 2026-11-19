package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 花豕蛊[Flower Boar Gu]的限时图标效果：提供持续 30 秒、相当于一猪之力的兽力点数，结束时无额外余波；真值由流派效果维
 * 护，本类仅承担图标、计时与 {@link BeastStrengthBuff} 标识，使点数作为增幅并入唯一力量结算体系（超出承受力的部分全额
 * 发挥并结算过载反噬）。
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
