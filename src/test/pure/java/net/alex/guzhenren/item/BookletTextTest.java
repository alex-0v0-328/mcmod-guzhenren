package net.alex.guzhenren.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookletTextTest {

    private static final String SPACE = "　";

    @Test
    @DisplayName("a figure's attribute line names its declared rank and path, as the language files do")
    void attributeLineMatchesTheFigure() throws IOException {
        JsonObject simplified = readLang("zh_cn");
        JsonObject traditional = readLang("zh_tw");
        for (Booklet booklet : Booklet.values()) {
            booklet.getFigure().ifPresent(figure -> {
                Booklet.Paragraph attribute = booklet.getParagraphs().getFirst();
                assertEquals(Booklet.Role.ATTRIBUTE, attribute.role(), booklet + " opens without its attribute line");
                assertEquals(String.join(SPACE, simplified.get(figure.rank().getTranslationKey()).getAsString(),
                        simplified.get(figure.path().getTranslationKey()).getAsString(), "蛊虫"),
                        attribute.simplified(), booklet.name());
                assertEquals(String.join(SPACE, traditional.get(figure.rank().getTranslationKey()).getAsString(),
                        traditional.get(figure.path().getTranslationKey()).getAsString(), "蠱蟲"),
                        attribute.traditional(), booklet.name());
            });
        }
    }

    @Test
    @DisplayName("a figure's caption is its item's name in zh_cn and zh_tw")
    void captionMatchesTheItemName() throws IOException {
        JsonObject simplified = readLang("zh_cn");
        JsonObject traditional = readLang("zh_tw");
        for (Booklet booklet : Booklet.values()) {
            booklet.getFigure().ifPresent(figure -> {
                String key = "item.guzhenren." + figure.id();
                assertNotNull(simplified.get(key), booklet + " shows an unnamed item " + figure.id());
                assertEquals(simplified.get(key).getAsString(), figure.caption(false), booklet.name());
                assertEquals(traditional.get(key).getAsString(), figure.caption(true), booklet.name());
            });
        }
    }

    @Test
    @DisplayName("booklet text is Han glyphs, vertical punctuation and the ideographic space only")
    void textUsesVerticalPunctuation() {
        for (Booklet booklet : Booklet.values()) {
            for (Booklet.Paragraph paragraph : booklet.getParagraphs()) {
                for (boolean traditional : new boolean[] { false, true }) {
                    String text = paragraph.glyphs(traditional);
                    text.codePoints().forEach(glyph -> assertTrue(isBookGlyph(glyph),
                            booklet + " sets " + Character.toString(glyph) + " in " + text));
                }
            }
        }
    }

    private static boolean isBookGlyph(int glyph) {
        return Character.UnicodeScript.of(glyph) == Character.UnicodeScript.HAN || glyph == 0x3000
                || glyph >= 0xFE10 && glyph <= 0xFE19 || glyph >= 0xFE30 && glyph <= 0xFE4F;
    }

    private static JsonObject readLang(String language) throws IOException {
        try (InputStream input = BookletTextTest.class.getResourceAsStream(
                "/assets/guzhenren/lang/" + language + ".json")) {
            assertNotNull(input, language + ".json missing");
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
