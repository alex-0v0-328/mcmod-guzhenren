package net.alex.guzhenren.core;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The two names every domain enum in this mod carries: its saved name and its translation key.
 *
 * <p>Contract for the closed vocabulary enums ({@code GuPath}, {@code Rank}, {@code Stage},
 * {@code Talent}, ...). An enum declares only its {@link #getTranslationPrefix}; the saved name is the
 * constant's name in lowercase, and the translation key is the prefix followed by the saved name. Both
 * language providers take this interface rather than a {@code String}, so the key an enum ships and the
 * key it is registered under can never drift into two different literals.
 *
 * <p>⚠ Each enum's {@code CODEC} writes the saved name into player saves: renaming a constant loses
 * the value already saved under the old name. Lowercasing uses {@link Locale#ROOT}, so every system
 * locale writes the same names.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public interface NamedEnum extends StringRepresentable {

    String getTranslationPrefix();

    @Override
    default @NotNull String getSerializedName() { return ((Enum<?>) this).name().toLowerCase(Locale.ROOT); }

    default String getTranslationKey() { return getTranslationPrefix() + getSerializedName(); }
}
