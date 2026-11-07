package net.alex.guzhenren.item;

import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * The texts of the booklets [书册], one constant per book, each paragraph in Simplified and Traditional glyphs.
 *
 * <p>Closed vocabulary enum: a book is a run of paragraphs set top to bottom, right to left, an optional figure
 * [图] on the left half-page, and nothing else. The text stays here rather than in the language files: it is
 * written once in Chinese and shown in Chinese in every language, Traditional under {@code zh_tw} and Simplified
 * everywhere else, and {@code tools/booklet_font.py} cuts the bitmap fonts from the string literals of this file. A
 * paragraph's two scripts hold the same number of glyphs, so both lay out alike. Punctuation is the vertical forms
 * (︐ ︒), one glyph cell each; the ideographic space {@code U+3000} keeps its cell and draws nothing.
 *
 * <p>A figure names the Gu it shows by registry id and declares that Gu's rank and path. The declaration is a drift
 * guard: {@code BookletGameTests} fails when the registered Gu no longer matches it, and {@code BookletTextTest}
 * fails when the attribute line [属性] or the caption no longer matches the language files.
 *
 * <p>⚠ A glyph added here ships only after {@code tools/booklet_font.py} reruns; {@code BookletFontTest} fails
 * until it does.
 *
 * @author Alex
 * @version 1.0.0
 * @see BookletItem
 * @since 1.0.0
 */

public enum Booklet {

    GU_ATLAS_HOPE(new Figure("hope_gu", Rank.ONE, GuPath.HUMAN, "希望蛊", "希望蠱"),
            new Paragraph(Role.ATTRIBUTE, "一转　人道　蛊虫", "一轉　人道　蠱蟲"),
            new Paragraph(Role.TEXT, "希望蛊只是一个小小的微弱光点︒你需要它为你开窍︐让你走上蛊师的修行之路︒",
                    "希望蠱只是一個小小的微弱光點︒你需要它為你開竅︐讓你走上蠱師的修行之路︒"),
            new Paragraph(Role.QUOTE, "人啊︐你只要把心交给我︐就可以了︒", "人啊︐你只要把心交給我︐就可以了︒"));

    private final @Nullable Figure figure;
    private final List<Paragraph> paragraphs;

    Booklet(@Nullable Figure figure, Paragraph... paragraphs) {
        this.figure = figure;
        this.paragraphs = List.of(paragraphs);
    }

    public Optional<Figure> getFigure() { return Optional.ofNullable(figure); }

    public List<Paragraph> getParagraphs() { return paragraphs; }

    public enum Role { ATTRIBUTE, TEXT, QUOTE }

    public record Paragraph(Role role, String simplified, String traditional) {

        public Paragraph {
            requireTwins(simplified, traditional);
        }

        public String glyphs(boolean traditional) { return traditional ? this.traditional : simplified; }
    }

    public record Figure(String id, Rank rank, GuPath path, String simplified, String traditional) {

        public Figure {
            requireTwins(simplified, traditional);
        }

        public String caption(boolean traditional) { return traditional ? this.traditional : simplified; }

        public Item getItem() { return BuiltInRegistries.ITEM.get(Guzhenren.id(id)); }
    }

    private static void requireTwins(String simplified, String traditional) {
        if (simplified.isEmpty()) throw new IllegalArgumentException("empty text");
        if (simplified.codePointCount(0, simplified.length()) != traditional.codePointCount(0, traditional.length())) {
            throw new IllegalArgumentException(simplified + " and " + traditional + " differ in length");
        }
    }
}
