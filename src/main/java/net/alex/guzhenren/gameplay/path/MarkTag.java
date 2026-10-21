package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * Where a Dao mark [道痕] came from, so a quantity can later be revoked exactly.
 *
 * <p>Closed vocabulary enum; no sibling mod may add a tag. That closure is what lets
 * {@code PathEntry} keep a plain {@code EnumMap} keyed by it.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum MarkTag implements NamedEnum {

    NATURAL,
    RACE,
    EXTREME_PHYSIQUE;

    public static final Codec<MarkTag> CODEC = StringRepresentable.fromEnum(MarkTag::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.tag."; }
}
