package net.alex.guzhenren.item;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronicleTextTest {

    @Test
    @DisplayName("each section's Simplified and Traditional lines pair up, code point for code point")
    void scriptsPairLineForLine() {
        for (Chronicle.Book book : Chronicle.Book.values()) {
            for (Chronicle section : Chronicle.sections(book)) {
                List<String> simplified = section.lines(false);
                List<String> traditional = section.lines(true);
                assertEquals(simplified.size(), traditional.size(), section.toString());
                assertTrue(simplified.size() > 1, section + " holds no text");
                for (int line = 0; line < simplified.size(); line++) {
                    String a = simplified.get(line);
                    String b = traditional.get(line);
                    assertEquals(a.codePointCount(0, a.length()), b.codePointCount(0, b.length()),
                            section + " line " + (line + 1));
                }
            }
        }
    }

    @Test
    @DisplayName("every section reads in both scripts into paragraphs of Han glyphs and vertical punctuation")
    void everySectionReads() {
        for (Chronicle section : Chronicle.sections(Chronicle.Book.HUMAN_ANCESTOR_LEGEND)) {
            for (boolean traditional : new boolean[] { false, true }) {
                List<List<String>> paragraphs = section.read(traditional);
                assertFalse(paragraphs.isEmpty(), section + " reads empty");
                for (List<String> paragraph : paragraphs) {
                    assertFalse(paragraph.isEmpty(), section + " keeps an empty paragraph");
                    for (String glyph : paragraph) {
                        assertTrue(isHan(glyph) || "﹁﹂﹃﹄︒︐︑︔︓︕︖︙︱".contains(glyph), section + " sets " + glyph);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("each mark takes its own cell in its vertical form; brackets turn into corner brackets")
    void marksTurnVertical() {
        assertEquals(List.of("人", "道", "︓", "﹁", "好", "︒", "﹂", "︐"),
                Chronicle.convert("人道：“好。”，"));
        assertEquals(List.of("噗", "︱", "︱", "︕"), Chronicle.convert("噗——！"));
        assertEquals(List.of("甲", "︙", "︙", "︖", "︑", "︔"), Chronicle.convert("甲……？、；"));
        assertEquals(List.of("﹃", "人", "﹄", "﹁", "原", "﹂"), Chronicle.convert("‘人’（原）"));
        assertEquals(List.of("﹃", "书", "﹄"), Chronicle.convert("《书》"));
        assertEquals(List.of("甲"), Chronicle.convert("　　甲"));
        assertThrows(IllegalArgumentException.class, () -> Chronicle.convert("甲.乙"));
    }

    @Test
    @DisplayName("every non-blank line after the title is a paragraph, a lone ellipsis included")
    void eachLineIsAParagraph() {
        assertEquals(List.of(List.of("甲", "︒"), List.of("︙", "︙")),
                Chronicle.read(List.of("题", "甲。", "　　", "……")));
        Chronicle first = new Chronicle(Chronicle.Book.HUMAN_ANCESTOR_LEGEND, 1);
        assertEquals(first.lines(false).size() - 1, first.read(false).size());
    }

    @Test
    @DisplayName("headings, colophons and item names spell the section in Han numerals")
    void sectionNamesSpellHanNumerals() {
        Chronicle eleventh = new Chronicle(Chronicle.Book.HUMAN_ANCESTOR_LEGEND, 11);
        assertEquals("人祖传第十一节", eleventh.heading(false));
        assertEquals("人祖傳第十一節終", eleventh.colophon(true));
        assertEquals("「人祖传」第十一节", eleventh.itemName(false));
        assertEquals("「人祖傳」第三十八節", new Chronicle(Chronicle.Book.HUMAN_ANCESTOR_LEGEND, 38).itemName(true));
        assertEquals("石人，你阻挡了我成功的路", eleventh.title(false));
        assertEquals(List.of("一", "十", "十一", "二十", "二十六", "九十九"),
                List.of(1, 10, 11, 20, 26, 99).stream().map(Chronicle::numeral).toList());
        assertThrows(IllegalArgumentException.class, () -> Chronicle.numeral(100));
        assertThrows(IllegalArgumentException.class, () -> new Chronicle(Chronicle.Book.HUMAN_ANCESTOR_LEGEND, 39));
    }

    private static boolean isHan(String glyph) {
        return Character.UnicodeScript.of(glyph.codePointAt(0)) == Character.UnicodeScript.HAN;
    }
}
