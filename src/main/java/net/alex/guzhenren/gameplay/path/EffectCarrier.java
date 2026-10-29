package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * Where Dao marks [道痕] sit on a person: the aperture [空窍], the body [肉体], the soul [魂魄] or the mind [脑海].
 *
 * <p>Closed vocabulary enum; no sibling mod may add a carrier. Every {@link PathEffect} names the one carrier it
 * works on. A bare mark, one without an effect ({@link PathEntry}), sits on the body and nowhere else.
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
