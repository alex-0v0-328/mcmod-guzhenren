package net.alex.guzhenren.core;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * The one weighted roll behind birth-graded enums: walk the values, spend the roll, return where it lands.
 *
 * <p>Weights come from a {@code ToIntFunction} so each enum keeps its own weight column. The filtered
 * overload skips values before weighing (the normal-talent roll excludes {@code EXTREME}); a total of
 * zero throws from {@code nextInt(0)}, exactly as before the roll was centralized here.
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

    public static <T> T pick(T[] values, Predicate<T> filter, ToIntFunction<T> weight) {
        return pick(values, ThreadLocalRandom.current(), filter, weight);
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
