package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 蛮力天牛蛊[Brute Force Longhorn Beetle Gu]的限时图标效果：提供持续 45 秒、相当于一牛之力的兽力点数，结束时无额外余
 * 波；真值由流派效果维护，本类仅承担图标、计时与 {@link BeastStrengthBuff} 标识，使点数并入唯一力量结算体系并正常受承受
 * 力限制（与熊力同点但比猪类更持久）。
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
