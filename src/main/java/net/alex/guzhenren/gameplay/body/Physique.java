package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * Cumulative body physiques [体质] that can coexist on a player.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum Physique implements NamedEnum {

    ZOMBIE,
    HALF_ZOMBIE,
    EXTREME;

    public static final Codec<Physique> CODEC = StringRepresentable.fromEnum(Physique::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.physique."; }
}
