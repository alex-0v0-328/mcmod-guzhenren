package net.alex.guzhenren.gameplay.immortalaperture.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地清点的同刻缓存：同一游戏刻里面板各段与托管轮共用一次扫描。清点什么由功能自己声明（{@link Census}），本类只管缓存。
 * <ul>
 *   <li>按维度与清点种类存，记清点时的游戏刻；{@link Census#ofLand} 的清点另记是哪份存档算的（GameTest 会拿自建存档配
 *       主世界），{@link Census#ofLevel} 的只看维度。</li>
 *   <li>⚠ 宰杀会从清点表里删动物：面板操作后（{@code EarthSpiritPanel#handle}）与托管轮后
 *       （{@code BlissfulLands#host}）必须 {@link #invalidate}；停服 {@link #clear}。</li>
 * </ul>
 */

public final class LandCensus {

    private LandCensus() {}

    /** 一种清点：{@code count} 给出这一刻的结果；{@code byLand} 为真时按存档分开缓存。 */
    public record Census<T>(@NotNull BiFunction<ServerLevel, BlissfulLandData, T> count, boolean byLand) {

        public static <T> @NotNull Census<T> ofLevel(@NotNull Function<ServerLevel, T> count) {
            return new Census<>((level, data) -> count.apply(level), false);
        }

        public static <T> @NotNull Census<T> ofLand(@NotNull BiFunction<ServerLevel, BlissfulLandData, T> count) {
            return new Census<>(count, true);
        }
    }

    private record Entry(long time, @Nullable BlissfulLandData data, Object value) {}
    private static final Map<ResourceKey<Level>, Map<Census<?>, Entry>> CACHE = new HashMap<>();

    public static <T> @NotNull T take(@NotNull ServerLevel level, @NotNull Census<T> census) {
        if (census.byLand()) throw new IllegalArgumentException("this census counts per land; pass the land data");
        return take(level, null, census);
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T take(@NotNull ServerLevel level, @Nullable BlissfulLandData data,
                                      @NotNull Census<T> census) {
        BlissfulLandData key = census.byLand() ? data : null;
        Map<Census<?>, Entry> cached = CACHE.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
        Entry entry = cached.get(census);
        if (entry != null && entry.time() == level.getGameTime() && entry.data() == key) return (T) entry.value();
        T value = census.count().apply(level, data);
        cached.put(census, new Entry(level.getGameTime(), key, value));
        return value;
    }

    public static void invalidate(@NotNull ServerLevel level) { CACHE.remove(level.dimension()); }

    public static void clear() { CACHE.clear(); }
}
