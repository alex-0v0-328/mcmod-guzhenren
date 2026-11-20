package net.alex.guzhenren.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.Chronicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookletFontTest {

    private static final String RERUN = "; rerun python3 -B tools/booklet_font.py";

    @Test
    @DisplayName("the Simplified font holds every glyph the booklets show outside zh_tw")
    void simplifiedFontCoversTheBooklets() throws IOException {
        assertCovers("booklet_hans", false);
    }

    @Test
    @DisplayName("the Traditional font holds every glyph the booklets show under zh_tw")
    void traditionalFontCoversTheBooklets() throws IOException {
        assertCovers("booklet_hant", true);
    }

    @Test
    @DisplayName("the 8-pixel margin fonts hold every glyph the chronicles print in their margins")
    void marginFontsCoverTheChronicles() throws IOException {
        for (boolean traditional : new boolean[] { false, true }) {
            Set<Integer> shipped = readShipped(traditional ? "booklet_margin_hant" : "booklet_margin_hans", 8);
            Set<String> missing = new TreeSet<>();
            for (Chronicle chronicle : Chronicle.sections(Chronicle.Book.RENZU_ZHUAN)) {
                for (BookletLayout.Spread spread : BookletLayout.layChronicle(chronicle, traditional)) {
                    spread.margin().stream().map(BookletLayout.Cell::glyph)
                            .filter(glyph -> !shipped.contains(glyph.codePointAt(0))).forEach(missing::add);
                }
            }
            assertTrue(missing.isEmpty(), "margin font lacks " + String.join("", missing) + RERUN);
        }
    }

    private static Set<Integer> readShipped(String font, int cell) throws IOException {
        JsonObject provider = readFont(font);
        assertEquals("bitmap", provider.get("type").getAsString());
        assertEquals(cell, provider.get("height").getAsInt(), "one glyph pixel to one GUI pixel" + RERUN);
        assertEquals(7, provider.get("ascent").getAsInt(), "a cell's top sits on the y it is drawn at" + RERUN);
        List<String> rows = new ArrayList<>();
        provider.getAsJsonArray("chars").forEach(row -> rows.add(row.getAsString()));
        BufferedImage sheet = readSheet(font);
        assertEquals(rows.getFirst().codePointCount(0, rows.getFirst().length()) * cell, sheet.getWidth(),
                font + RERUN);
        assertEquals(rows.size() * cell, sheet.getHeight(), font + RERUN);
        return rows.stream().flatMapToInt(String::codePoints).boxed().collect(Collectors.toSet());
    }

    private static void assertCovers(String font, boolean traditional) throws IOException {
        Set<Integer> shipped = readShipped(font, 12);
        Set<String> missing = new TreeSet<>();
        for (Booklet booklet : Booklet.values()) {
            List<String> texts = new ArrayList<>();
            booklet.getParagraphs().forEach(paragraph -> texts.add(paragraph.glyphs(traditional)));
            booklet.getFigure().ifPresent(figure -> texts.add(figure.caption(traditional)));
            texts.forEach(text -> text.codePoints().filter(glyph -> !Character.isWhitespace(glyph))
                    .filter(glyph -> !shipped.contains(glyph)).forEach(glyph -> missing.add(Character.toString(glyph))));
        }
        for (Chronicle chronicle : Chronicle.sections(Chronicle.Book.RENZU_ZHUAN)) {
            for (BookletLayout.Spread spread : BookletLayout.layChronicle(chronicle, traditional)) {
                spread.text().stream().map(BookletLayout.Cell::glyph)
                        .filter(glyph -> !shipped.contains(glyph.codePointAt(0))).forEach(missing::add);
            }
        }
        assertTrue(missing.isEmpty(), font + " lacks " + String.join("", missing) + RERUN);
    }

    private static JsonObject readFont(String font) throws IOException {
        try (InputStream input = BookletFontTest.class.getResourceAsStream("/assets/guzhenren/font/" + font + ".json")) {
            assertNotNull(input, font + ".json missing" + RERUN);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals(1, json.getAsJsonArray("providers").size());
            return json.getAsJsonArray("providers").get(0).getAsJsonObject();
        }
    }

    private static BufferedImage readSheet(String font) throws IOException {
        try (InputStream input = BookletFontTest.class.getResourceAsStream(
                "/assets/guzhenren/textures/font/" + font + ".png")) {
            assertNotNull(input, font + ".png missing" + RERUN);
            return ImageIO.read(input);
        }
    }
}
