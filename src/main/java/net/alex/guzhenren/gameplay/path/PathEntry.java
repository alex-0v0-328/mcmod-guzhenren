package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModStreamCodecs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One Path's [流派] standing: its attainment grade, plus the tagged tallies that got it there.
 *
 * <p>Leaf record nested inside {@link PathData}; immutable. Carries a sparse {@link MarkTag}-keyed
 * map of Dao marks, pruned of zero-or-below entries by the compact constructor.
 *
 * <p>⚠ {@code markTotal()} is a sum over the tag map and is NEVER stored: a breakdown and a total
 * cannot contradict each other when only the breakdown exists. ⚠
 * The tag map is the complete Dao-mark breakdown for this path.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathData
 * @see PathService
 * @since 1.0.0
 */

public record PathEntry(GuAttainment attainment, Map<MarkTag, Long> marks) {

    public static final PathEntry DEFAULT = new PathEntry(GuAttainment.NONE, Map.of());
    private static final Codec<Map<MarkTag, Long>> TAGS = Codec.unboundedMap(MarkTag.CODEC, Codec.LONG);
    public static final Codec<PathEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GuAttainment.CODEC.optionalFieldOf("attainment", GuAttainment.NONE).forGetter(PathEntry::attainment),
            TAGS.optionalFieldOf("marks", Map.of()).forGetter(PathEntry::marks)
    ).apply(instance, PathEntry::new));
    public static final StreamCodec<ByteBuf, PathEntry> STREAM_CODEC = StreamCodec.composite(
            ModStreamCodecs.ofEnum(GuAttainment.class), PathEntry::attainment,
            ModStreamCodecs.enumMap(MarkTag.class, ByteBufCodecs.VAR_LONG), PathEntry::marks,
            PathEntry::new);

    public PathEntry {
        marks = normalized(marks);
    }

    public long markTotal() { return sum(marks); }

    public long mark(MarkTag tag) { return marks.getOrDefault(tag, 0L); }

    public PathEntry withAttainment(GuAttainment attainment) { return new PathEntry(attainment, marks); }

    public PathEntry withMark(MarkTag tag, long value) { return new PathEntry(attainment, set(marks, tag, value)); }

    public boolean isDefault() { return attainment == GuAttainment.NONE && marks.isEmpty(); }

    private static long sum(Map<MarkTag, Long> tags) {
        long total = 0L;
        for (long value : tags.values()) total = LongMath.saturatedAdd(total, value);
        return total;
    }

    private static Map<MarkTag, Long> normalized(Map<MarkTag, Long> tags) {
        Map<MarkTag, Long> pruned = new EnumMap<>(MarkTag.class);
        tags.forEach((tag, value) -> {
            if (value != null && value > 0L) pruned.put(tag, value);
        });
        return Collections.unmodifiableMap(pruned);
    }

    private static Map<MarkTag, Long> set(Map<MarkTag, Long> tags, MarkTag tag, long value) {
        Map<MarkTag, Long> next = new EnumMap<>(MarkTag.class);
        next.putAll(tags);
        next.put(tag, Math.max(0L, value));
        return next;
    }
}
