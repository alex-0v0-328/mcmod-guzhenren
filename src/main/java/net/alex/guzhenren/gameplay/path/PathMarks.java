package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * A count of path marks [道痕] on one path: whole marks [条] and strands [丝], two independent counters.
 *
 * <p>Immutable value record; both counters floor at zero. ⚠ Nothing converts one counter into the other: strands
 * never roll up into a mark, and no code path may add one. A race or a physique lays down whole marks; a mortal Gu
 * lays down strands only. Sums saturate per counter.
 *
 * <p>{@link #MAP_CODEC} writes the two counters as the fields {@code marks} and {@code strands}, so a holder can
 * inline them; both read leniently, so a field of another shape left by an older save reads as zero instead of
 * failing the whole record.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEntry
 * @see PathEffect
 * @since 1.0.0
 */

public record PathMarks(long marks, long strands) {

    public static final PathMarks NONE = new PathMarks(0L, 0L);
    public static final MapCodec<PathMarks> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.lenientOptionalFieldOf("marks", 0L).forGetter(PathMarks::marks),
            Codec.LONG.lenientOptionalFieldOf("strands", 0L).forGetter(PathMarks::strands)
    ).apply(instance, PathMarks::new));
    public static final Codec<PathMarks> CODEC = MAP_CODEC.codec();

    public PathMarks {
        marks = Math.max(0L, marks);
        strands = Math.max(0L, strands);
    }

    public static @NotNull PathMarks ofMarks(long marks) { return new PathMarks(marks, 0L); }

    public static @NotNull PathMarks ofStrands(long strands) { return new PathMarks(0L, strands); }

    public static @NotNull PathMarks decode(@NotNull FriendlyByteBuf buf) {
        return new PathMarks(buf.readVarLong(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathMarks value) {
        buf.writeVarLong(value.marks());
        buf.writeVarLong(value.strands());
    }

    public boolean isEmpty() { return marks == 0L && strands == 0L; }

    public PathMarks add(PathMarks other) {
        return new PathMarks(LongMath.saturatedAdd(marks, other.marks), LongMath.saturatedAdd(strands, other.strands));
    }

    public PathMarks minus(PathMarks amount) {
        return new PathMarks(LongMath.saturatedSubtract(marks, amount.marks),
                LongMath.saturatedSubtract(strands, amount.strands));
    }

    public PathMarks withMarks(long value) { return new PathMarks(value, strands); }

    public PathMarks withStrands(long value) { return new PathMarks(marks, value); }
}
