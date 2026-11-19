package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 力道结算在单次查询中的不可变快照：由 {@link StrengthService#snapshot} 一次性采集肉身、流派效果与增幅，避免单次结算重
 * 复遍历状态。
 * <ul>
 *   <li>{@code totalPoints} 为总力量点[Strength Points]（永久点数与增幅点数之和，不含任何倍率）；{@code punchFactor} 为
 *       各增幅整拳倍率的乘积（无增幅时为 {@code 1}）；未激活全力以赴蛊[All-Out Effort Gu]或未入专精分支时
 *       {@code allOutTier} 为 {@link StrengthService#NO_ALL_OUT}。</li>
 *   <li>{@link #settle} 按结算公式得出结算力量点：解放兽种的永久点数再计一份，再乘整拳倍率与蓄力倍率后向下取整；
 *       {@link #settledPoints} 为不含解放与蓄力的平时读数（客户端与服务端数值一致）。</li>
 *   <li>{@link #overloadPoints} 为结算力量点超出当前承受力的部分，全额发挥并由 {@link BodyOverload} 结算过载反噬。</li>
 *   <li>⚠ 必须通过工厂方法 {@link #of} 构建，以确保 {@code totalPoints}、{@code capacity} 与 {@code punchFactor} 三个派
 *       生字段与输入分量严格一致。</li>
 * </ul>
 */

public record StrengthSnapshot(int normalCapacity, int permanentPoints, @NotNull List<StrengthAmplifier> amplifiers,
                               int allOutTier, int totalPoints, int capacity, double punchFactor) {

    public StrengthSnapshot {
        amplifiers = List.copyOf(amplifiers);
    }

    public static @NotNull StrengthSnapshot of(int normalCapacity, int permanentPoints,
                                               @NotNull List<StrengthAmplifier> amplifiers, int allOutTier) {
        return new StrengthSnapshot(normalCapacity, permanentPoints, amplifiers, allOutTier,
                permanentPoints + StrengthService.getAmplifierPoints(amplifiers),
                StrengthService.getCapacity(normalCapacity, StrengthService.getCapacityBonus(amplifiers), allOutTier),
                StrengthService.getPunchFactor(amplifiers));
    }

    public boolean allOut() { return allOutTier != StrengthService.NO_ALL_OUT; }

    public int settle(int releasedPoints, double chargeFactor) {
        return StrengthService.getSettledPoints(totalPoints, releasedPoints, punchFactor * chargeFactor);
    }

    public int settledPoints() { return settle(0, 1.0D); }

    public int overloadPoints(int settledPoints) {
        return StrengthService.getOverloadPoints(settledPoints, capacity);
    }
}
