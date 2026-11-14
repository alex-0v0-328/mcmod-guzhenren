package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 力道结算某一时刻的读数：{@link StrengthService#snapshot} 一次收集，一次结算只读一遍肉体、路径效果与增幅。
 * <ul>
 *   <li>全力以赴蛊[All-Out Effort Gu] tier 未在持分支身体上运行时为 {@link StrengthService#NO_ALL_OUT}；肉体过载为限时
 *       兽力超当前承受力的点数（{@link StrengthService#getOverloadPoints}），仍全额发挥。</li>
 *   <li>不含普通截断外的解放（依赖轮次），由
 *       {@link StrengthService#getUsablePoints(net.minecraft.world.entity.player.Player, StrengthSnapshot)} 加上。</li>
 *   <li>⚠ 须经 {@link #of} 构建，保证派生与收集分量一致；规范构造器不检查。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService
 * @since 1.0.0
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
}
