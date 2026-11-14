package net.alex.guzhenren.gameplay.clock;

/**
 * 加快玩家自身时间的唯一接口：{@link TimeFlow#getSelfRate} 遍历 {@code getActiveEffects()} 累加 {@link #timeRate}，
 * 不点名效果。效果侧实现、时钟侧只读，时钟不依赖效果包；维度倍率 {@code ModDimensions.AnchoredDimension#timeRate}
 * 不走此接口。
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @since 1.0.0
 */

public interface TimeRateContributor {

    int timeRate(int amplifier);
}
