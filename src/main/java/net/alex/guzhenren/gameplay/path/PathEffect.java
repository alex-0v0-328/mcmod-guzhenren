package net.alex.guzhenren.gameplay.path;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人身上的一个流派效果。
 *
 * <ul>
 *   <li>存于 {@link PathEffectData}，运行时只由 {@link PathEffectService} 写入；来源取蛊物品 id 或先天 id；
 *       {@code endTick} 为游戏时间或 {@link #PERMANENT}。</li>
 *   <li>{@code paths} 按道[Path]记道痕[Path Marks]条与丝（{@link PathMarks}），可多道；增幅者给所增效果补律道丝。</li>
 *   <li>⚠ 恰在带道痕时存在：构造器剪无道痕的道，存储丢无道的效果；只有在与不在，无物拿走部分道痕，抹去即摘除。</li>
 *   <li>作用只读类型、payload 和层数；延时（{@link #withEndTick}）不加道痕，每层（{@link #addLayer}、
 *       {@link #dropLayer}）带该层道痕；替换连道痕一起换。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectType
 * @see PathEffectService
 * @since 1.0.0
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
