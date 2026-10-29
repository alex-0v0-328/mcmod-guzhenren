package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * One Path's [流派] standing: its attainment grade, plus its bare Dao marks [道痕], whole marks and strands.
 *
 * <p>Leaf record nested inside {@link PathData}; immutable. Bare marks sit on the body [肉体], the only carrier a
 * mark without an effect has, and do nothing. The two counters save inline beside the attainment.
 *
 * <p>⚠ This is only the bare half of a path's marks: the marks carried by effects live in {@link PathEffectData},
 * and {@code PathService.getMarks} adds the two. Neither half stores a total.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathData
 * @see PathService
 * @since 1.0.0
 */

public record PathEntry(GuAttainment attainment, DaoMarks marks) {

    public static final PathEntry DEFAULT = new PathEntry(GuAttainment.NONE, DaoMarks.NONE);
    public static final Codec<PathEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GuAttainment.CODEC.optionalFieldOf("attainment", GuAttainment.NONE).forGetter(PathEntry::attainment),
            DaoMarks.MAP_CODEC.forGetter(PathEntry::marks)
    ).apply(instance, PathEntry::new));

    public static @NotNull PathEntry decode(@NotNull FriendlyByteBuf buf) {
        return new PathEntry(ModByteBufs.readEnum(buf, GuAttainment.class), DaoMarks.decode(buf));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathEntry value) {
        ModByteBufs.writeEnum(buf, value.attainment());
        DaoMarks.encode(buf, value.marks());
    }

    public PathEntry withAttainment(GuAttainment attainment) { return new PathEntry(attainment, marks); }

    public PathEntry withMarks(DaoMarks value) { return new PathEntry(attainment, value); }

    public boolean isDefault() { return attainment == GuAttainment.NONE && marks.isEmpty(); }
}
