package net.alex.guzhenren.gameplay.path;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * One path effect [流派效果] on a person: what works, on which carrier, from what source, carrying which Dao marks
 * [道痕] on which paths, until when, in how many layers, with its kind's own payload.
 *
 * <p>Immutable record kept in {@link PathEffectData}; {@link PathEffectService} is the only runtime writer. The
 * source is the Gu item's id for a Gu's effect, or an innate id for one the body was born with. {@code endTick}
 * is a game time, or {@link #PERMANENT}. {@code paths} holds each path's {@link DaoMarks}, whole marks and strands
 * apart; one effect may carry several paths -- an amplifier adds Rule strands to the effect it amplifies.
 *
 * <p>⚠ An effect is on exactly while it carries marks: the compact constructor prunes paths with none, and the
 * store drops an effect with no path left. A partial cut changes the bookkeeping only; what the effect does reads
 * its type, payload and layers, never its marks. Extending the time ({@link #withEndTick}) adds no marks; a layer
 * ({@link #addLayer}) adds that layer's marks, so they grow with the layer count; replacing an effect replaces its
 * marks with it.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectType
 * @see PathEffectService
 * @since 1.0.0
 */

public record PathEffect<P>(PathEffectType<P> type, EffectCarrier carrier, ResourceLocation source,
                            Map<GuPath, DaoMarks> paths, long endTick, int layers, P payload) {

    public static final long PERMANENT = -1L;

    public PathEffect {
        paths = normalize(paths);
        layers = Math.max(1, layers);
    }

    public static <P> @NotNull PathEffect<P> of(@NotNull PathEffectType<P> type, @NotNull EffectCarrier carrier,
                                                @NotNull ResourceLocation source, @NotNull Map<GuPath, DaoMarks> paths,
                                                long endTick, @NotNull P payload) {
        return new PathEffect<>(type, carrier, source, paths, endTick, 1, payload);
    }

    public DaoMarks getMarks(GuPath path) { return paths.getOrDefault(path, DaoMarks.NONE); }

    public DaoMarks getTotal() {
        DaoMarks total = DaoMarks.NONE;
        for (DaoMarks marks : paths.values()) total = total.add(marks);
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

    public PathEffect<P> withEndTick(long value) {
        return new PathEffect<>(type, carrier, source, paths, value, layers, payload);
    }

    public PathEffect<P> withPayload(P value) {
        return new PathEffect<>(type, carrier, source, paths, endTick, layers, value);
    }

    public PathEffect<P> withPaths(Map<GuPath, DaoMarks> value) {
        return new PathEffect<>(type, carrier, source, value, endTick, layers, payload);
    }

    public PathEffect<P> addMarks(Map<GuPath, DaoMarks> added) {
        Map<GuPath, DaoMarks> next = copy();
        added.forEach((path, marks) -> next.merge(path, marks, DaoMarks::add));
        return withPaths(next);
    }

    public PathEffect<P> addLayer(Map<GuPath, DaoMarks> layerMarks) {
        return new PathEffect<>(type, carrier, source, addMarks(layerMarks).paths, endTick,
                layers == Integer.MAX_VALUE ? layers : layers + 1, payload);
    }

    public PathEffect<P> cut(GuPath path, DaoMarks amount) {
        Map<GuPath, DaoMarks> next = copy();
        next.put(path, getMarks(path).cut(amount));
        return withPaths(next);
    }

    private Map<GuPath, DaoMarks> copy() {
        Map<GuPath, DaoMarks> next = new EnumMap<>(GuPath.class);
        next.putAll(paths);
        return next;
    }

    private static Map<GuPath, DaoMarks> normalize(Map<GuPath, DaoMarks> paths) {
        Map<GuPath, DaoMarks> pruned = new EnumMap<>(GuPath.class);
        paths.forEach((path, marks) -> {
            if (marks != null && !marks.isEmpty()) pruned.put(path, marks);
        });
        return Collections.unmodifiableMap(pruned);
    }
}
