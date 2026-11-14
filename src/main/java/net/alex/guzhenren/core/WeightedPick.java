package net.alex.guzhenren.core;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * 出生定级枚举共用的加权抽取。
 * <ul>
 *   <li>权重取自 {@code ToIntFunction}，各枚举自带权重列；过滤重载先剔除再加权（普通资质[Aptitude]抽取排除
 *       {@code TEN_EXTREMES}）；总权重 0 时 {@code nextInt(0)} 抛异常，同集中前。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WeightedPick {

    private WeightedPick() {}

    public static <T> T pick(T[] values, ToIntFunction<T> weight) {
        return pick(values, ThreadLocalRandom.current(), value -> true, weight);
    }

    public static <T> T pick(T[] values, RandomGenerator random, Predicate<T> filter, ToIntFunction<T> weight) {
        int total = 0;
        for (T v : values) {
            if (filter.test(v)) total += weight.applyAsInt(v);
        }
        int roll = random.nextInt(total);
        for (T v : values) {
            if (!filter.test(v)) continue;
            roll -= weight.applyAsInt(v);
            if (roll < 0) return v;
        }
        throw new IllegalStateException("weighted pick exhausted");
    }
}
