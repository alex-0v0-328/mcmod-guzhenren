package net.alex.guzhenren.datagen.lang;

import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

/**
 * The Traditional Chinese [繁中] strings, derived from the Simplified table.
 *
 * <p>Extends {@link net.alex.guzhenren.datagen.lang.ZhCnLanguageProvider} for {@code zh_tw}. The two
 * differ in glyphs only, so this class adds no entry of its own: it overrides {@link #add(String, String)} and
 * passes every value through {@link net.alex.guzhenren.datagen.lang.TraditionalGlyphs#convert}.
 * Every {@code add*()} of {@link net.neoforged.neoforge.common.data.LanguageProvider} funnels into
 * that one method, items, blocks and enum keys alike.
 *
 * <p>⚠ Never add a {@code zh_tw}-only string here. A wrong glyph is fixed in the glyph table, which
 * fixes it everywhere it occurs; an override for one key would be the first place the two Chinese
 * files drift apart.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.lang.TraditionalGlyphs
 * @since 1.0.0
 */

public class ZhTwLanguageProvider extends ZhCnLanguageProvider {

    public ZhTwLanguageProvider(PackOutput output) {
        super(output, "zh_tw");
    }

    @Override
    public void add(@NotNull String key, @NotNull String value) {
        super.add(key, TraditionalGlyphs.convert(value));
    }
}
