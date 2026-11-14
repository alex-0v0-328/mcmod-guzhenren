package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 道痕[Path Marks]在人身上的载体：空窍[Aperture]、肉体、魂魄或脑海。闭合枚举；每个 {@link PathEffect}
 * 只作用于一个载体，纯道痕（{@link PathEntry}）只在肉体上。
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEntry
 * @see PathEffect
 * @since 1.0.0
 */

public enum EffectCarrier implements NamedEnum {

    APERTURE,
    BODY,
    SOUL,
    MIND;

    public static final Codec<EffectCarrier> CODEC = StringRepresentable.fromEnum(EffectCarrier::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.carrier."; }
}
