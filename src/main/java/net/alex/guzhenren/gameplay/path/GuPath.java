package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * Every Dao path [流派] a Gu can belong to. There is no {@code NONE}; "has not chosen" is a {@code null}.
 *
 * <p>Closed vocabulary enum and the canonical example of the mod's closure rule: no sibling mod may
 * add a constant here. That guarantee is what lets every path-keyed store stay a plain {@code EnumMap}.
 *
 * <p>⚠ Never derive a Chinese rendering from the English one. Space Path [宇道] and Time Path [宙道]
 * both exist, so a guess at either does not fail loudly -- it quietly names the other real path.
 *
 * @author Alex
 * @version 1.0.0
 * @see MarkTag
 * @see GuAttainment
 * @since 1.0.0
 */

public enum GuPath implements NamedEnum {

    HEAVEN, RULE, SPACE, TIME, HUMAN,

    METAL, WOOD, WATER, FIRE, EARTH, ICE_SNOW, LIGHTNING, CLOUD,
    QI, SOUND, LIGHT, DARK, POISON,

    STRENGTH, DREAM, REFINEMENT, WISDOM, INFORMATION, THEFT,
    LUCK, KILLING, BLOOD, SOUL, ENSLAVEMENT,
    FOOD, FORMATION, PAINTING, TRANSFORMATION;
    public static final Codec<GuPath> CODEC = StringRepresentable.fromEnum(GuPath::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.path."; }
}
