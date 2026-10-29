package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * Whether a path effect [流派效果] helps its carrier or harms it: a path buff [流派增益] or a path debuff [流派减益].
 *
 * <p>Closed vocabulary enum; every {@link PathEffectType} names one. The B panel lists the two apart, each under its
 * own name, and later rules may read it; it changes no bookkeeping.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectType
 * @since 1.0.0
 */

public enum PathEffectCategory implements NamedEnum {

    BENEFICIAL,
    HARMFUL;

    public static final Codec<PathEffectCategory> CODEC = StringRepresentable.fromEnum(PathEffectCategory::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.effect_category."; }
}
