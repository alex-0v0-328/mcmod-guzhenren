package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 一条道[Path]上的道痕[Path Marks]数：条与点，两个独立计数。
 *
 * <ul>
 *   <li>值 record，两计数下限为零，求和按计数各自饱和；种族或体质留下整条，凡蛊只留点。</li>
 *   <li>⚠ 两计数从不互换：点永不进位成条，任何代码不得新增这种换算。</li>
 *   <li>{@link #MAP_CODEC} 把两计数写成字段 {@code marks}、{@code points}，持有者可内联；两字段宽松读取，
 *       旧存档其他形状的字段读作零，整条记录不会读失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEntry
 * @see PathEffect
 * @since 1.0.0
 */

public record PathMarks(long marks, long points) {

    public static final PathMarks NONE = new PathMarks(0L, 0L);
    public static final MapCodec<PathMarks> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.lenientOptionalFieldOf("marks", 0L).forGetter(PathMarks::marks),
            Codec.LONG.lenientOptionalFieldOf("points", 0L).forGetter(PathMarks::points)
    ).apply(instance, PathMarks::new));
    public static final Codec<PathMarks> CODEC = MAP_CODEC.codec();

    public PathMarks {
        marks = Math.max(0L, marks);
        points = Math.max(0L, points);
    }

    public static @NotNull PathMarks ofMarks(long marks) { return new PathMarks(marks, 0L); }

    public static @NotNull PathMarks ofPoints(long points) { return new PathMarks(0L, points); }

    public static @NotNull PathMarks decode(@NotNull FriendlyByteBuf buf) {
        return new PathMarks(buf.readVarLong(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathMarks value) {
        buf.writeVarLong(value.marks());
        buf.writeVarLong(value.points());
    }

    public boolean isEmpty() { return marks == 0L && points == 0L; }

    public PathMarks add(PathMarks other) {
        return new PathMarks(LongMath.saturatedAdd(marks, other.marks), LongMath.saturatedAdd(points, other.points));
    }

    public PathMarks minus(PathMarks amount) {
        return new PathMarks(LongMath.saturatedSubtract(marks, amount.marks),
                LongMath.saturatedSubtract(points, amount.points));
    }

    public PathMarks withMarks(long value) { return new PathMarks(value, points); }

    public PathMarks withPoints(long value) { return new PathMarks(marks, value); }
}
