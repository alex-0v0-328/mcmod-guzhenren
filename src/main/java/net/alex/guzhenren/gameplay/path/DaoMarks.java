package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * A count of Dao marks [道痕] on one path: whole marks [条] and strands [丝], two independent counters.
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

public record DaoMarks(long marks, long strands) {

    public static final DaoMarks NONE = new DaoMarks(0L, 0L);
    public static final MapCodec<DaoMarks> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.lenientOptionalFieldOf("marks", 0L).forGetter(DaoMarks::marks),
            Codec.LONG.lenientOptionalFieldOf("strands", 0L).forGetter(DaoMarks::strands)
    ).apply(instance, DaoMarks::new));
    public static final Codec<DaoMarks> CODEC = MAP_CODEC.codec();

    public DaoMarks {
        marks = Math.max(0L, marks);
        strands = Math.max(0L, strands);
    }

    public static @NotNull DaoMarks ofMarks(long marks) { return new DaoMarks(marks, 0L); }

    public static @NotNull DaoMarks ofStrands(long strands) { return new DaoMarks(0L, strands); }

    public static @NotNull DaoMarks decode(@NotNull FriendlyByteBuf buf) {
        return new DaoMarks(buf.readVarLong(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull DaoMarks value) {
        buf.writeVarLong(value.marks());
        buf.writeVarLong(value.strands());
    }

    public boolean isEmpty() { return marks == 0L && strands == 0L; }

    public DaoMarks add(DaoMarks other) {
        return new DaoMarks(LongMath.saturatedAdd(marks, other.marks), LongMath.saturatedAdd(strands, other.strands));
    }

    public DaoMarks cut(DaoMarks amount) {
        return new DaoMarks(LongMath.saturatedSubtract(marks, amount.marks),
                LongMath.saturatedSubtract(strands, amount.strands));
    }

    public DaoMarks withMarks(long value) { return new DaoMarks(value, strands); }

    public DaoMarks withStrands(long value) { return new DaoMarks(marks, value); }
}
