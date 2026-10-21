package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The composition of a thought [念] pool: which kind a portion is tagged as.
 *
 * <p>Closed vocabulary enum; the tagged map on {@code MindData} keys off it. No sibling mod may add a tag.
 *
 * <p>⚠ {@code NATURAL} is derived, never stored -- it is what remains once every tagged portion is
 * subtracted from the pool's current. Only {@code EVIL} (and future tags) live in the map, so
 * {@code NATURAL} has no writer.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ThoughtTag implements NamedEnum {

    NATURAL,
    EVIL;

    public static final Codec<ThoughtTag> CODEC = StringRepresentable.fromEnum(ThoughtTag::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.tag."; }
}
