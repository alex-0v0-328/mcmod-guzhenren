package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The four branches of Strength Path [力道], including Normal [基础力道] as the default classification.
 *
 * <p>Closed vocabulary enum. Beast Strength Phantom and Human Jun Strength own today's persistent
 * strength data; Atmospheric Heaven and Earth has no implementation yet; Normal classifies every
 * other Strength Path Gu without adding another data field.
 *
 * <p>Mark Interference [道痕互斥]: {@link #interferesWith} is symmetric, and a body holding one branch is refused
 * any branch it interferes with. Today only Beast Strength Phantom and Human Jun Strength interfere; a new pair
 * is one more case here.
 *
 * @author Alex
 * @version 1.0.0
 * @see HumanStrength
 * @see BeastStrength
 * @since 1.0.0
 */

public enum StrengthPathBranch implements NamedEnum {

    BEAST_STRENGTH_PHANTOM,
    HUMAN_JUN_STRENGTH,
    ATMOSPHERIC_HEAVEN_AND_EARTH,
    NORMAL;

    public static final Codec<StrengthPathBranch> CODEC = StringRepresentable.fromEnum(StrengthPathBranch::values);

    public boolean interferesWith(StrengthPathBranch other) {
        return switch (this) {
            case BEAST_STRENGTH_PHANTOM -> other == HUMAN_JUN_STRENGTH;
            case HUMAN_JUN_STRENGTH -> other == BEAST_STRENGTH_PHANTOM;
            case ATMOSPHERIC_HEAVEN_AND_EARTH, NORMAL -> false;
        };
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.strength_path_branch."; }
}
