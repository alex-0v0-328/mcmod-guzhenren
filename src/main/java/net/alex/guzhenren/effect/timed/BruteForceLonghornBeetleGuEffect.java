package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 蛮力天牛蛊[Brute Force Longhorn Beetle Gu]限时增益：一牛之力力量点[Strength Points]，45 秒，结束无余波；
 * 真值为流派效果，本效果仅图标与计时；点数经 {@link BeastStrengthBuff} 入唯一力量结算，攻击总值仍为一数，承受力照算；
 * 牛熊同点，比猪持久，时长即特性。
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
