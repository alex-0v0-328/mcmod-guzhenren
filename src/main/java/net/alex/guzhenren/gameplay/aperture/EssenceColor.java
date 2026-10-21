package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The color name an essence [真元] bar takes at each rank [转数], echoed by the Relics Gu [舍利蛊] names.
 *
 * <p>Closed vocabulary enum carried by {@link Rank}; the bar is NOT tinted by it (one bar cycling ten
 * hues reads as status, not rank). No sibling mod may add a color.
 *
 * <p>⚠ Zero callers today, but it is not dead code: it names the relic rungs and keeps the rank table
 * self-describing. Do not delete it.
 *
 * @author Alex
 * @version 1.0.0
 * @see Rank
 * @since 1.0.0
 */

public enum EssenceColor implements NamedEnum {

    NONE,
    GREEN_COPPER,
    RED_STEEL,
    WHITE_SILVER,
    YELLOW_GOLDEN,
    PURPLE_CRYSTAL,
    GREEN_GRAPE,
    RED_DATE,
    WHITE_LITCHI,
    YELLOW_APRICOT;

    public static final Codec<EssenceColor> CODEC = StringRepresentable.fromEnum(EssenceColor::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.essence_color."; }
}
