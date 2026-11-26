package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 玩家身上可并存的肉身体质标记枚举（存于 {@link BodyData#physiques}，其中 {@link #ZOMBIE} 与
 * {@link #TEMPORARY_ZOMBIE} 互斥，任一均可与 {@link #EXTREME} 并存）。
 */

public enum Physique implements NamedEnum {

    ZOMBIE,
    TEMPORARY_ZOMBIE,
    EXTREME;

    public static final Codec<Physique> CODEC = StringRepresentable.fromEnum(Physique::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.physique."; }
}
