package net.alex.guzhenren.gameplay.clock;

/**
 * 提升玩家自身时间流速的药水效果接口：{@link TimeFlow#getSelfRate} 遍历玩家活动效果并累加此接口返回的倍率，使
 * {@code gameplay.clock} 无需依赖 {@code effect} 包；维度时间倍率（{@code ModDimensions.AnchoredDimension#timeRate}）不
 * 走此接口。
 */

public interface TimeRateContributor {

    int timeRate(int amplifier);
}
