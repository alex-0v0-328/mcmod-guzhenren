package net.alex.guzhenren.client.screen;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.alex.guzhenren.item.Booklet.Paragraph;
import net.alex.guzhenren.item.Booklet.Role;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.Chronicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.client.screen.BookletLayout.AREA_HEIGHT;
import static net.alex.guzhenren.client.screen.BookletLayout.AREA_WIDTH;
import static net.alex.guzhenren.client.screen.BookletLayout.COLUMNS;
import static net.alex.guzhenren.client.screen.BookletLayout.GLYPH_X;
import static net.alex.guzhenren.client.screen.BookletLayout.LEFT_X;
import static net.alex.guzhenren.client.screen.BookletLayout.PITCH;
import static net.alex.guzhenren.client.screen.BookletLayout.RIGHT_X;
import static net.alex.guzhenren.client.screen.BookletLayout.ROWS;
import static net.alex.guzhenren.client.screen.BookletLayout.TOP;
import static net.alex.guzhenren.client.screen.BookletLayout.getColumnX;
import static net.alex.guzhenren.client.screen.BookletLayout.getRowY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookletLayoutTest {

    private static final int PAPER = 0xFFE0D0A5;
    private static final int RULE = 0xFFB3A27C;
    private static final String HAN = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥";

    @Test
    @DisplayName("the spread is 272x188 with two 112x150 text areas of 8 columns by 12 rows")
    void spreadMatchesTheTextureContract() {
        assertEquals(272, BookletLayout.WIDTH);
        assertEquals(188, BookletLayout.HEIGHT);
        assertEquals(AREA_WIDTH, COLUMNS * PITCH);
        assertEquals(144, RIGHT_X);
        assertEquals(16, LEFT_X);
        assertEquals(24, TOP);
        assertEquals(27, getRowY(0));
        assertEquals(171, getRowY(ROWS - 1) + 12, "the last row's cell ends 3 pixels above the area's foot");
    }

    @Test
    @DisplayName("the sprite's text areas hold only paper and the rules at 14k, k = 1 to 7")
    void spriteRulesTheColumnsWhereTheLayoutDoes() throws IOException {
        BufferedImage spread = readSpread();
        for (int area : new int[] { RIGHT_X, LEFT_X }) {
            for (int y = TOP; y < TOP + AREA_HEIGHT; y++) {
                for (int x = area; x < area + AREA_WIDTH; x++) {
                    int expected = x != area && (x - area) % PITCH == 0 ? RULE : PAPER;
                    assertEquals(Integer.toHexString(expected), Integer.toHexString(spread.getRGB(x, y)),
                            "pixel " + x + "," + y);
                }
            }
            assertNotEqualsPaper(spread, area - 1);
            assertNotEqualsPaper(spread, area + AREA_WIDTH);
        }
    }

    @Test
    @DisplayName("every column's ink, x + 2 to x + 12, keeps one blank pixel to the rule or frame on either side")
    void inkKeepsOnePixelOfPaperOnEitherSide() throws IOException {
        BufferedImage spread = readSpread();
        for (int column = 0; column < 2 * COLUMNS; column++) {
            int ink = getColumnX(column) + GLYPH_X;
            for (int y = TOP; y < TOP + AREA_HEIGHT; y++) {
                assertEquals(PAPER, spread.getRGB(ink - 1, y), "column " + column + " left of its ink");
                assertEquals(PAPER, spread.getRGB(ink + 11, y), "column " + column + " right of its ink");
            }
            assertNotEqualsPaper(spread, getColumnX(column) + PITCH);
        }
    }

    @Test
    @DisplayName("columns run right to left from the right half-page's rightmost, then the left half-page")
    void columnsRunRightToLeftRightHalfFirst() {
        assertEquals(242, getColumnX(0));
        assertEquals(144, getColumnX(7));
        assertEquals(114, getColumnX(8));
        assertEquals(16, getColumnX(15));
    }

    @Test
    @DisplayName("a full column flows on into the next, top-aligned")
    void textFlowsIntoTheNextColumn() {
        List<BookletLayout.Cell> cells = lay(new Paragraph(Role.TEXT, HAN.substring(0, 13), HAN.substring(0, 13)));
        assertEquals(13, cells.size());
        assertEquals(cell(HAN.substring(11, 12), 0, 11), cells.get(11));
        assertEquals(cell(HAN.substring(12, 13), 1, 0), cells.get(12));
    }

    @Test
    @DisplayName("a quote sinks two rows in every column it fills")
    void quoteSinksEveryColumn() {
        List<BookletLayout.Cell> cells = lay(new Paragraph(Role.QUOTE, HAN.substring(0, 11), HAN.substring(0, 11)));
        assertEquals(cell(HAN.substring(0, 1), 0, 2), cells.getFirst());
        assertEquals(cell(HAN.substring(9, 10), 0, 11), cells.get(9));
        assertEquals(cell(HAN.substring(10, 11), 1, 2), cells.get(10));
    }

    @Test
    @DisplayName("each paragraph starts a fresh column, a quote after others one blank column further")
    void paragraphsStartFreshColumns() {
        List<BookletLayout.Cell> cells = lay(new Paragraph(Role.ATTRIBUTE, "甲乙", "甲乙"),
                new Paragraph(Role.TEXT, "丙", "丙"), new Paragraph(Role.QUOTE, "丁", "丁"));
        assertEquals(cell("丙", 1, 0), cells.get(2));
        assertEquals(cell("丁", 3, 2), cells.getLast());
        assertEquals(cell("丁", 0, 2), lay(new Paragraph(Role.QUOTE, "丁", "丁")).getFirst(),
                "a quote opening the text keeps the first column");
    }

    @Test
    @DisplayName("an ideographic space keeps its cell and draws nothing")
    void ideographicSpaceKeepsItsCell() {
        assertEquals(List.of(cell("甲", 0, 0), cell("乙", 0, 2)), lay(new Paragraph(Role.ATTRIBUTE, "甲　乙", "甲　乙")));
    }

    @Test
    @DisplayName("punctuation never opens a column: the glyph before it moves down with it [避头]")
    void punctuationPullsTheGlyphBeforeIt() {
        String text = HAN.substring(0, 12) + "︒" + HAN.charAt(12);
        List<BookletLayout.Cell> cells = lay(new Paragraph(Role.TEXT, text, text));
        assertEquals(cell(HAN.substring(10, 11), 0, 10), cells.get(10));
        assertEquals(cell(HAN.substring(11, 12), 1, 0), cells.get(11));
        assertEquals(cell("︒", 1, 1), cells.get(12));

        String run = HAN.substring(0, 11) + "︒︐" + HAN.charAt(11);
        List<BookletLayout.Cell> pulled = lay(new Paragraph(Role.TEXT, run, run));
        assertEquals(cell(HAN.substring(10, 11), 1, 0), pulled.get(10));
        assertEquals(cell("︐", 1, 2), pulled.get(12));
    }

    @Test
    @DisplayName("text past its columns, a long attribute or an opening punctuation refuses to lay out")
    void overflowRefuses() {
        String half = HAN.repeat(5).substring(0, COLUMNS * ROWS);
        assertDoesNotThrow(() -> BookletLayout.layText(List.of(new Paragraph(Role.TEXT, half, half)), COLUMNS,
                false));
        assertThrows(IllegalArgumentException.class, () -> BookletLayout.layText(
                List.of(new Paragraph(Role.TEXT, half + "甲", half + "甲")), COLUMNS, false));
        String spread = half + half;
        assertDoesNotThrow(() -> lay(new Paragraph(Role.TEXT, spread, spread)));
        assertThrows(IllegalArgumentException.class, () -> lay(new Paragraph(Role.TEXT, spread + "甲", spread + "甲")));
        String attribute = HAN.substring(0, ROWS + 1);
        assertThrows(IllegalArgumentException.class, () -> lay(new Paragraph(Role.ATTRIBUTE, attribute, attribute)));
        assertThrows(IllegalArgumentException.class, () -> lay(new Paragraph(Role.TEXT, "︐甲", "︐甲")));
    }

    @Test
    @DisplayName("a figure centers its 48-pixel icon and caption on the left text area")
    void figureCentersOnTheLeftArea() {
        BookletLayout.Plate plate = BookletLayout.plate("希望蛊");
        assertEquals(48, plate.iconX());
        assertEquals(51, plate.iconY());
        assertEquals(List.of(new BookletLayout.Cell("希", 67, 111), new BookletLayout.Cell("望", 67, 123),
                new BookletLayout.Cell("蛊", 67, 135)), plate.caption());
        assertThrows(IllegalArgumentException.class, () -> BookletLayout.plate(HAN.substring(0, 8)));
    }

    @Test
    @DisplayName("Gu Atlas: Hope fills seven right-hand columns, 1 + 3 + blank + 2, alike in both scripts")
    void guAtlasHopeFillsSevenColumns() {
        for (boolean traditional : new boolean[] { false, true }) {
            BookletLayout.Spread spread = BookletLayout.lay(Booklet.GU_ATLAS_HOPE, traditional);
            assertNotNull(spread.plate());
            int[] perColumn = new int[COLUMNS];
            for (BookletLayout.Cell cell : spread.text()) perColumn[(RIGHT_X + AREA_WIDTH - cell.x()) / PITCH]++;
            assertEquals(List.of(6, 12, 12, 12, 0, 10, 7, 0),
                    Arrays.stream(perColumn).boxed().toList(), "script traditional=" + traditional);
            assertEquals(getRowY(2), spread.text().get(6 + 36).y(), "the quote sinks two rows");
        }
    }

    @Test
    @DisplayName("every booklet lays out in both scripts, on the right half-page beside a figure or a recipe grid")
    void everyBookletLaysOut() {
        for (Booklet booklet : Booklet.values()) {
            for (boolean traditional : new boolean[] { false, true }) {
                BookletLayout.Spread spread = assertDoesNotThrow(() -> BookletLayout.lay(booklet, traditional),
                        booklet.name());
                if (booklet.getFigure().isEmpty()) assertNull(spread.plate());
                if (booklet.getFigure().isEmpty() && booklet.getRecipe().isEmpty()) continue;
                assertAll(spread.text().stream().map(cell -> () ->
                        assertTrue(cell.x() >= RIGHT_X, booklet + " sets " + cell + " on the figure page")));
            }
        }
    }

    @Test
    @DisplayName("a chronicle opens on its heading, then its title two rows low, then the text on a fresh column "
            + "indented two rows")
    void chronicleOpensOnHeadingAndTitle() {
        List<BookletLayout.Spread> spreads = BookletLayout.layChronicle(section(1), false);
        List<List<BookletLayout.Cell>> columns = columns(spreads.getFirst());
        assertEquals("人祖传第一节", glyphs(columns.get(0)));
        assertEquals(getRowY(0), columns.get(0).getFirst().y());
        assertEquals("困境和希望", glyphs(columns.get(1)));
        assertEquals(getRowY(2), columns.get(1).getFirst().y());
        assertEquals("传说︐这个世界刚刚形", glyphs(columns.get(2)));
        assertEquals(getRowY(BookletLayout.INDENT), columns.get(2).getFirst().y());
        assertEquals(getRowY(0), columns.get(3).getFirst().y());
    }

    @Test
    @DisplayName("a long title wraps two rows low; the text ends, skips a column and closes on the colophon")
    void titleWrapsAndColophonCloses() {
        List<BookletLayout.Spread> spreads = BookletLayout.layChronicle(section(11), false);
        List<List<BookletLayout.Cell>> opening = columns(spreads.getFirst());
        assertEquals("石人︐你阻挡了我成功", glyphs(opening.get(1)));
        assertEquals("的路", glyphs(opening.get(2)));
        assertEquals(getRowY(2), opening.get(2).getFirst().y());
        List<Integer> filled = new ArrayList<>();
        List<List<BookletLayout.Cell>> all = new ArrayList<>();
        for (int leaf = 0; leaf < spreads.size(); leaf++) {
            for (List<BookletLayout.Cell> column : columns(spreads.get(leaf))) {
                all.add(column);
                filled.add(getColumnIndex(leaf, column.getFirst().x()));
            }
        }
        assertEquals("人祖传第十一节终", glyphs(all.getLast()));
        assertEquals(getRowY(0), all.getLast().getFirst().y());
        assertEquals(2, filled.getLast() - filled.get(filled.size() - 2), "one blank column before the colophon");
        for (int at = 1; at < filled.size() - 1; at++) assertEquals(filled.get(at - 1) + 1, filled.get(at));
    }

    @Test
    @DisplayName("every chronicle section lays out in both scripts without a closing mark opening a column, an "
            + "opening bracket ending one or a dash split across two")
    void chroniclesKeepBracketsOffColumnEdges() {
        for (Chronicle chronicle : Chronicle.sections(Chronicle.Book.HUMAN_ANCESTOR_LEGEND)) {
            for (boolean traditional : new boolean[] { false, true }) {
                List<BookletLayout.Spread> spreads = assertDoesNotThrow(
                        () -> BookletLayout.layChronicle(chronicle, traditional), chronicle.toString());
                String last = "";
                for (BookletLayout.Spread spread : spreads) {
                    assertNull(spread.plate());
                    for (List<BookletLayout.Cell> column : columns(spread)) {
                        String first = column.getFirst().glyph();
                        assertTrue(column.getFirst().y() != getRowY(0) || !BookletLayout.NO_COLUMN_START.contains(first),
                                chronicle + " opens " + column);
                        assertTrue(!"︱".equals(last) || !"︱".equals(first), chronicle + " splits a dash at " + column);
                        last = column.getLast().glyph();
                        assertTrue(!"﹁﹃".contains(column.getLast().glyph()), chronicle + " ends " + column);
                        assertTrue(column.size() <= ROWS, chronicle + " overfills " + column);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("each chronicle paragraph starts a fresh column two rows low and runs on from the top")
    void paragraphsStartIndentedColumns() {
        Chronicle first = section(1);
        int indented = 0;
        for (BookletLayout.Spread spread : BookletLayout.layChronicle(first, false)) {
            for (List<BookletLayout.Cell> column : columns(spread)) {
                if (column.getFirst().y() == getRowY(BookletLayout.INDENT)) indented++;
            }
        }
        assertEquals(1 + first.read(false).size(), indented, "the title column plus one per paragraph");
    }

    private static Chronicle section(int number) { return new Chronicle(Chronicle.Book.HUMAN_ANCESTOR_LEGEND, number); }

    private static int getColumnIndex(int leaf, int glyphX) {
        for (int column = 0; column < 2 * COLUMNS; column++) {
            if (getColumnX(column) + GLYPH_X == glyphX) return leaf * 2 * COLUMNS + column;
        }
        throw new AssertionError("no column at x " + glyphX);
    }

    private static List<List<BookletLayout.Cell>> columns(BookletLayout.Spread spread) {
        Map<Integer, List<BookletLayout.Cell>> byX = new TreeMap<>(Comparator.reverseOrder());
        for (BookletLayout.Cell cell : spread.text()) byX.computeIfAbsent(cell.x(), x -> new ArrayList<>()).add(cell);
        byX.values().forEach(column -> column.sort(Comparator.comparingInt(BookletLayout.Cell::y)));
        return List.copyOf(byX.values());
    }

    private static String glyphs(List<BookletLayout.Cell> cells) {
        return String.join("", cells.stream().map(BookletLayout.Cell::glyph).toList());
    }

    private static List<BookletLayout.Cell> lay(Paragraph... paragraphs) {
        return BookletLayout.layText(List.of(paragraphs), 2 * COLUMNS, false);
    }

    private static BookletLayout.Cell cell(String glyph, int column, int row) {
        return new BookletLayout.Cell(glyph, getColumnX(column) + GLYPH_X, getRowY(row));
    }

    private static void assertNotEqualsPaper(BufferedImage spread, int x) {
        for (int y = TOP; y < TOP + AREA_HEIGHT; y++) {
            if (spread.getRGB(x, y) == PAPER) throw new AssertionError("x " + x + " should rule or frame the area");
        }
    }

    private static BufferedImage readSpread() throws IOException {
        try (InputStream input = BookletLayoutTest.class.getResourceAsStream(
                "/assets/guzhenren/textures/gui/sprites/book_spread.png")) {
            assertNotNull(input, "book_spread.png missing");
            return ImageIO.read(input);
        }
    }
}
