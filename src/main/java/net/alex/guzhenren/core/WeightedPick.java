package net.alex.guzhenren.core;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * 出生定级等枚举共用的加权随机抽取工具。权重由调用方传入的 {@link ToIntFunction} 提供，带过滤谓词的重载会先剔除不满足条
 * 件的候选项再累加总权重（例如抽取常规资质[Aptitude]时排除十绝体 {@code TEN_EXTREMES}）；若过滤后总权重为 0 则由
 * {@code random.nextInt(0)} 直接抛出异常。
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
