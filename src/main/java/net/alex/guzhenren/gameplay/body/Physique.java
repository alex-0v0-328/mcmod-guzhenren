package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 玩家身上可并存的累积体质。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum Physique implements NamedEnum {

    ZOMBIE,
    TEMP_ZOMBIE,
    EXTREME;

    public static final Codec<Physique> CODEC = StringRepresentable.fromEnum(Physique::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.physique."; }
}
