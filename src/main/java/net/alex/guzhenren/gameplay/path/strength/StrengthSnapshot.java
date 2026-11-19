package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 力道结算在单次查询中的不可变快照：由 {@link StrengthService#snapshot} 一次性采集肉身、流派效果与临时增幅，避免单次结
 * 算重复遍历状态。
 * <ul>
 *   <li>未激活全力以赴蛊[All-Out Effort Gu]或未入专精分支时 {@code allOutTier} 为 {@link StrengthService#NO_ALL_OUT}；
 *       {@code overloadPoints} 为限时兽力[Beast Strength]超出当前承受力的点数（全额发挥并触发肉体过载）。</li>
 *   <li>{@link #settledPoints} 为不含连招轮收尾全面解放的常态可发挥力量点[Strength Points]（客户端与服务端数值一致，供横
 *       冲系冲撞 {@link CrashSources} 读取）；最终可发挥点数由
 *       {@link StrengthService#getUsablePoints(Player, StrengthSnapshot)} 再叠入服务端收尾招解放增量。</li>
 *   <li>⚠ 必须通过工厂方法 {@link #of} 构建，以确保 {@code totalPoints}、{@code capacity} 与 {@code overloadPoints} 三
 *       个派生字段与输入分量严格一致。</li>
 * </ul>
 */

public record StrengthSnapshot(int normalCapacity, int permanentPoints, @NotNull List<StrengthAmplifier> amplifiers,
                               int allOutTier, int totalPoints, int capacity, int overloadPoints) {

    public StrengthSnapshot {
        amplifiers = List.copyOf(amplifiers);
    }

    public static @NotNull StrengthSnapshot of(int normalCapacity, int permanentPoints, int permanentBeastPoints,
                                               @NotNull List<StrengthAmplifier> amplifiers, int allOutTier) {
        int capacity = StrengthService.getCapacity(normalCapacity, StrengthService.getCapacityBonus(amplifiers),
                allOutTier);
        return new StrengthSnapshot(normalCapacity, permanentPoints, amplifiers, allOutTier,
                permanentPoints + StrengthService.getAmplifierPoints(amplifiers), capacity,
                StrengthService.getOverloadPoints(capacity, permanentBeastPoints,
                        StrengthService.getTimedBeastPoints(amplifiers)));
    }

    public boolean allOut() { return allOutTier != StrengthService.NO_ALL_OUT; }

    public int settledPoints() { return StrengthService.getUsablePoints(capacity, totalPoints) + overloadPoints; }
}
