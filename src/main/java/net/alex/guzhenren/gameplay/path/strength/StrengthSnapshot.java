package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 力道结算在单次查询中的不可变快照：由 {@link StrengthService#snapshot} 一次性采集肉身、流派效果与增幅，避免单次结算重
 * 复遍历状态。
 * <ul>
 *   <li>{@code capacity} 为承受力（只有一个数，增幅与全力以赴都不改它）；{@code totalPoints} 为总力量点[Strength
 *       Points]（永久点数与增幅点数之和，不含任何倍率）；{@code damageFactor} 为各增幅伤害倍率的乘积（无增幅时为
 *       {@code 1}）。</li>
 *   <li>{@link #settle} 按结算公式得出结算力量点：总力量点加暴击兽种点数，再乘伤害倍率与蓄力倍率后向下取整；
 *       {@link #settledPoints} 为不含暴击与蓄力的平时读数（客户端与服务端数值一致）。</li>
 *   <li>{@link #overloadPoints} 为结算力量点超出承受力的部分，全额发挥并由 {@link BodyOverload} 结算过载反噬。</li>
 *   <li>⚠ 必须通过工厂方法 {@link #of} 构建，以确保 {@code totalPoints} 与 {@code damageFactor} 两个派生字段与输入分量
 *       严格一致。</li>
 * </ul>
 */

public record StrengthSnapshot(int capacity, int permanentPoints, @NotNull List<StrengthAmplifier> amplifiers,
                               int totalPoints, double damageFactor) {

    public StrengthSnapshot {
        amplifiers = List.copyOf(amplifiers);
    }

    public static @NotNull StrengthSnapshot of(int capacity, int permanentPoints,
                                               @NotNull List<StrengthAmplifier> amplifiers) {
        return new StrengthSnapshot(capacity, permanentPoints, amplifiers,
                permanentPoints + StrengthService.getAmplifierPoints(amplifiers),
                StrengthService.getDamageFactor(amplifiers));
    }

    public int settle(int critPoints, double chargeFactor) {
        return StrengthService.getSettledPoints(totalPoints, critPoints, damageFactor * chargeFactor);
    }

    public int settledPoints() { return settle(0, 1.0D); }

    public int overloadPoints(int settledPoints) {
        return StrengthService.getOverloadPoints(settledPoints, capacity);
    }
}
