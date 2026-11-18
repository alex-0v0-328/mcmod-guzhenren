package net.alex.guzhenren.gameplay.clock;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * 每秒心跳对玩家时钟的单次采样快照，供整拍内各子系统复用而不再重复查询时钟。
 * <ul>
 *   <li>{@link #days} 为自上一拍以来主世界日钟跨过的整天数，供蛊虫每日饱食[Hunger]结算；{@link #rate} 为拍初采样的
 *       {@link TimeFlow#getRate} 总时间倍率，{@link #scale} 与 {@link #getSteps} 直接绑定该倍率委派给
 *       {@link TimeFlow}。</li>
 * </ul>
 */

public record Beat(long days, double rate, @NotNull RandomSource random) {

    public long scale(long amount) { return TimeFlow.scale(rate, amount, random); }

    public double scale(double amount) { return TimeFlow.scale(rate, amount); }

    public int getSteps() { return TimeFlow.getSteps(rate, random); }
}
