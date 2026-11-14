package net.alex.guzhenren.gameplay.clock;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * 心跳对玩家时钟的单次读数，整拍共用。{@link #days}：上拍以来日时钟走过的整天数，供蛊饱食[Hunger]遍历扣；
 * {@link #rate}：拍初的 {@link TimeFlow#getRate}；动词同 {@link TimeFlow}，绑定此倍率，不再问时钟。
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @since 1.0.0
 */

public record Beat(long days, double rate, @NotNull RandomSource random) {

    public long scale(long amount) { return TimeFlow.scale(rate, amount, random); }

    public double scale(double amount) { return TimeFlow.scale(rate, amount); }

    public int getSteps() { return TimeFlow.getSteps(rate, random); }
}
