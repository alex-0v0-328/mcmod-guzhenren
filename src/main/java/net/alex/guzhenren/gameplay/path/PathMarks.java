package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 单条流派上的道痕[Path Marks]计数记录：由「整条道痕（{@code marks}）」与「道痕点（{@code points}）」两个非负独立计数组
 * 成。
 * <ul>
 *   <li>加减运算对两个计数分别执行饱和算术（下限为 {@code 0}）；先天种族与十绝体[Ten Extreme Physiques]赋予整条道痕，凡
 *       蛊则赋予道痕点。</li>
 *   <li>⚠ 「条」与「点」完全独立、永不互相换算或进位，任何业务代码不得自行定义点到条的进位比例。</li>
 *   <li>{@link #MAP_CODEC} 将两个计数内联为 {@code marks} 与 {@code points} 可选容错字段（旧存档中不兼容的字段类型会被
 *       安静回退为 {@code 0}，不会导致外层记录反序列化失败）。</li>
 * </ul>
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
