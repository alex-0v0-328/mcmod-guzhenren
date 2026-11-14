package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 七窍：两眼、两耳、两鼻孔、口，按四类存，成对的一类共用一档伤势。
 * <ul>
 *   <li>都长在 {@link BodyPart#HEAD} 上，但伤势与头分开记：头无伤也可失明。</li>
 *   <li>七窍改造档等第一只七窍蛊再加，现在只有伤势。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Anatomy
 * @since 1.0.0
 */

public enum Organ implements NamedEnum {

    EYES,
    EARS,
    NOSE,
    MOUTH;

    public static final Codec<Organ> CODEC = StringRepresentable.fromEnum(Organ::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.organ."; }
}
