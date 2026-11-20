package net.alex.guzhenren.item;

import java.util.List;
import net.alex.guzhenren.item.Chronicle.Mark;
import net.alex.guzhenren.item.Chronicle.Token;
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
    @DisplayName("every section reads in both scripts into Han glyphs, corner brackets and paragraph circles")
    void everySectionReads() {
        for (Chronicle section : Chronicle.sections(Chronicle.Book.RENZU_ZHUAN)) {
            for (boolean traditional : new boolean[] { false, true }) {
                List<Token> tokens = section.read(traditional);
                assertTrue(tokens.getFirst().isHan() || Chronicle.OPENINGS.contains(tokens.getFirst().glyph()),
                        section + " opens on " + tokens.getFirst());
                for (Token token : tokens) {
                    assertTrue(token.isHan() || "﹁﹂﹃﹄○".contains(token.glyph()), section + " sets " + token);
                    if (!token.isHan()) assertEquals(Mark.NONE, token.mark(), section + " marks " + token);
                }
            }
        }
    }

    @Test
    @DisplayName("stops and pauses ride on the Han glyph before them, past brackets; a stop outranks a pause")
    void marksRideOnTheGlyphBefore() {
        assertEquals(List.of(han("人", Mark.NONE), han("道", Mark.PAUSE), bracket("﹁"), han("好", Mark.STOP),
                bracket("﹂")), Chronicle.convert("人道：“好。”，"));
        assertEquals(List.of(han("噗", Mark.STOP)), Chronicle.convert("噗——！"));
        assertEquals(List.of(han("甲", Mark.STOP)), Chronicle.convert("甲……，"));
        assertEquals(List.of(bracket("﹃"), han("人", Mark.NONE), bracket("﹄"), bracket("﹁"), han("原", Mark.NONE),
                bracket("﹂")), Chronicle.convert("‘人’（原）"));
        assertEquals(List.of(bracket("﹃"), han("书", Mark.NONE), bracket("﹄")), Chronicle.convert("《书》"));
        assertEquals(List.of(han("甲", Mark.NONE)), Chronicle.convert("　　甲"));
        assertEquals(List.of(), Chronicle.convert("……"), "a mark with no glyph before it drops");
        assertThrows(IllegalArgumentException.class, () -> Chronicle.convert("甲.乙"));
    }

    @Test
    @DisplayName("paragraphs run on, each after the first behind a circle; a paragraph without Han drops")
    void paragraphsRunOnBehindCircles() {
        Chronicle first = new Chronicle(Chronicle.Book.RENZU_ZHUAN, 1);
        List<Token> tokens = first.read(false);
        long paragraphs = first.lines(false).stream().skip(1)
                .filter(line -> Chronicle.convert(line).stream().anyMatch(Token::isHan)).count();
        assertEquals(paragraphs - 1, tokens.stream().filter(token -> token.glyph().equals("○")).count());
        assertFalse(tokens.getLast().glyph().equals("○"));
    }

    @Test
    @DisplayName("headings, colophons, margins and item names spell the section in Han numerals")
    void sectionNamesSpellHanNumerals() {
        Chronicle eleventh = new Chronicle(Chronicle.Book.RENZU_ZHUAN, 11);
        assertEquals("人祖传第十一节", eleventh.heading(false));
        assertEquals("人祖傳第十一節終", eleventh.colophon(true));
        assertEquals("第十一节", eleventh.margin(false));
        assertEquals("「人祖传」第十一节", eleventh.itemName(false));
        assertEquals("「人祖傳」第三十八節", new Chronicle(Chronicle.Book.RENZU_ZHUAN, 38).itemName(true));
        assertEquals("石人，你阻挡了我成功的路", eleventh.title(false));
        assertEquals(List.of("一", "十", "十一", "二十", "二十六", "九十九"),
                List.of(1, 10, 11, 20, 26, 99).stream().map(Chronicle::numeral).toList());
        assertThrows(IllegalArgumentException.class, () -> Chronicle.numeral(100));
        assertThrows(IllegalArgumentException.class, () -> new Chronicle(Chronicle.Book.RENZU_ZHUAN, 39));
    }

    private static Token han(String glyph, Mark mark) { return new Token(glyph, mark); }

    private static Token bracket(String glyph) { return new Token(glyph, Mark.NONE); }
}
