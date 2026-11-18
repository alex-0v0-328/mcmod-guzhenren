package net.alex.guzhenren.gameplay.path;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 挂载在玩家身上的单个流派效果不可变记录（存于 {@link PathEffectData}，运行时仅由 {@link PathEffectService} 写入）。
 * <ul>
 *   <li>{@code endTick} 为到期游戏时刻或 {@link #PERMANENT}（永久）；{@code source} 标识来源（蛊虫取物品注册 ID，先天取
 *       {@code guzhenren:innate/race} 或 {@code guzhenren:innate/extreme_physique}，力量取
 *       {@code guzhenren:strength/<名称>}）。</li>
 *   <li>{@code paths} 按流派记录该效果携带的道痕[Path Marks]条数与点数（支持跨多条流派，例如增幅蛊会在被增幅效果上追加
 *       律道道痕点）。</li>
 *   <li>⚠ 流派效果的存在性与其携带的道痕严格绑定：紧凑构造器自动剔除零道痕的流派，{@link PathEffectData} 自动丢弃无任何
 *       道痕的效果；不存在脱离效果扣减部分道痕的操作，道痕归零即代表效果被移除。</li>
 *   <li>具体机制只读效果类型、{@code payload} 与叠层数（{@code layers}）；延长持续时间（{@link #withEndTick}）不增加道
 *       痕，增减层数（{@link #addLayer} / {@link #dropLayer}）同步增减单层道痕，同槽替换则连同道痕整体覆盖。</li>
 * </ul>
 */

public record PathEffect<P>(PathEffectType<P> type, EffectCarrier carrier, ResourceLocation source,
                            Map<GuPath, PathMarks> paths, long endTick, int layers, P payload) {

    public static final long PERMANENT = -1L;

    public PathEffect {
        paths = normalize(paths);
        layers = Math.max(1, layers);
    }

    public static <P> @NotNull PathEffect<P> of(@NotNull PathEffectType<P> type, @NotNull EffectCarrier carrier,
                                                @NotNull ResourceLocation source, @NotNull Map<GuPath, PathMarks> paths,
                                                long endTick, @NotNull P payload) {
        return new PathEffect<>(type, carrier, source, paths, endTick, 1, payload);
    }

    public PathMarks getMarks(GuPath path) { return paths.getOrDefault(path, PathMarks.NONE); }

    public PathMarks getTotal() {
        PathMarks total = PathMarks.NONE;
        for (PathMarks marks : paths.values()) total = total.add(marks);
        return total;
    }

    public boolean isEmpty() { return paths.isEmpty(); }

    public boolean isPermanent() { return endTick == PERMANENT; }

    public boolean hasRunOut(long now) { return !isPermanent() && now >= endTick; }

    public long getTicksLeft(long now) { return isPermanent() ? Long.MAX_VALUE : Math.max(0L, endTick - now); }

    public boolean sharesSlot(@NotNull PathEffect<?> other) {
        return type == other.type && (type.isExclusive() || source.equals(other.source));
    }

    public @NotNull Component getName() { return type.getName(this); }

    public @Nullable PathEffectType.Projection getProjection() { return type.project(this); }

    public PathEffect<P> withEndTick(long value) {
        return new PathEffect<>(type, carrier, source, paths, value, layers, payload);
    }

    private PathEffect<P> withPaths(Map<GuPath, PathMarks> value) {
        return new PathEffect<>(type, carrier, source, value, endTick, layers, payload);
    }

    public PathEffect<P> addMarks(Map<GuPath, PathMarks> added) {
        Map<GuPath, PathMarks> next = copy();
        added.forEach((path, marks) -> next.merge(path, marks, PathMarks::add));
        return withPaths(next);
    }

    public PathEffect<P> addLayer(Map<GuPath, PathMarks> layerMarks) {
        return new PathEffect<>(type, carrier, source, addMarks(layerMarks).paths, endTick,
                layers == Integer.MAX_VALUE ? layers : layers + 1, payload);
    }

    public PathEffect<P> dropLayer(Map<GuPath, PathMarks> layerMarks) {
        Map<GuPath, PathMarks> next = copy();
        layerMarks.forEach((path, marks) -> next.put(path, getMarks(path).minus(marks)));
        return new PathEffect<>(type, carrier, source, next, endTick, layers - 1, payload);
    }

    private Map<GuPath, PathMarks> copy() {
        Map<GuPath, PathMarks> next = new EnumMap<>(GuPath.class);
        next.putAll(paths);
        return next;
    }

    private static Map<GuPath, PathMarks> normalize(Map<GuPath, PathMarks> paths) {
        Map<GuPath, PathMarks> pruned = new EnumMap<>(GuPath.class);
        paths.forEach((path, marks) -> {
            if (marks != null && !marks.isEmpty()) pruned.put(path, marks);
        });
        return Collections.unmodifiableMap(pruned);
    }
}
