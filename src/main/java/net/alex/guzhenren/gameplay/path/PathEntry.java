package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 一条道[Path]的现状：造诣，加纯道痕[Path Marks]的条与丝。
 *
 * <ul>
 *   <li>叶子 record 嵌于 {@link PathData}，两计数随造诣内联存档；纯道痕在肉体上（无效果道痕的唯一载体），无作用。</li>
 *   <li>⚠ 这里只存纯道痕一半：效果道痕在 {@link PathEffectData}，由 {@code PathService.getMarks} 相加；不存总数。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathData
 * @see PathService
 * @since 1.0.0
 */

public record PathEntry(GuAttainment attainment, PathMarks marks) {

    public static final PathEntry DEFAULT = new PathEntry(GuAttainment.NONE, PathMarks.NONE);
    public static final Codec<PathEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GuAttainment.CODEC.optionalFieldOf("attainment", GuAttainment.NONE).forGetter(PathEntry::attainment),
            PathMarks.MAP_CODEC.forGetter(PathEntry::marks)
    ).apply(instance, PathEntry::new));

    public static @NotNull PathEntry decode(@NotNull FriendlyByteBuf buf) {
        return new PathEntry(ModByteBufs.readEnum(buf, GuAttainment.class), PathMarks.decode(buf));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathEntry value) {
        ModByteBufs.writeEnum(buf, value.attainment());
        PathMarks.encode(buf, value.marks());
    }

    public PathEntry withAttainment(GuAttainment attainment) { return new PathEntry(attainment, marks); }

    public PathEntry withMarks(PathMarks value) { return new PathEntry(attainment, value); }

    public boolean isDefault() { return attainment == GuAttainment.NONE && marks.isEmpty(); }
}
