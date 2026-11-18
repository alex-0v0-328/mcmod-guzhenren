package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 单条流派的进度记录（嵌于 {@link PathData}）：包含流派造诣（{@link GuAttainment}）与肉身裸道痕[Path Marks]
 * （{@link PathMarks}）。
 * <ul>
 *   <li>裸道痕仅存于肉体且本身不产生具体效果；造诣与裸道痕在同一对象内联存档。</li>
 *   <li>⚠ 此处仅保存不随效果附着的裸道痕；流派效果附带的道痕存于 {@link PathEffectData}，查询玩家某流派的总道痕必须调用
 *       {@link PathService#getMarks} 将两部分相加。</li>
 * </ul>
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
