package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.item.Booklet;
import org.jetbrains.annotations.Nullable;

/**
 * Where each glyph of a {@link Booklet} sits on its spread: top to bottom down a column, columns right to left, the
 * right half-page first from its rightmost column, then the left half-page, like an opened thread-bound book [线装书].
 *
 * <p>The spread sprite is {@value #WIDTH}x{@value #HEIGHT}, drawn unscaled. Each half-page's text area is
 * {@value #AREA_WIDTH}x{@value #AREA_HEIGHT} with its top-left at ({@value #RIGHT_X},{@value #TOP}) on the right and
 * ({@value #LEFT_X},{@value #TOP}) on the left: {@value #COLUMNS} columns of {@value #ROWS} rows, {@value #PITCH}
 * pixels a column and {@value #ROW_HEIGHT} a row. The sprite rules the columns [界栏] one pixel wide at x = 144+14k
 * and 16+14k for k = 1 to 7. A glyph is a 12x12 cell of the booklet font, its ink in the cell's columns 0 to 10, drawn
 * {@value #GLYPH_X} pixels right of its column's start and {@value #GLYPH_Y} pixels below the area's top plus its
 * row: one blank pixel stays between the ink and the rule on either side, and between the rightmost column's ink and
 * the frame [版框].
 *
 * <p>Each paragraph starts a fresh column and flows into the next when one fills. An attribute line [属性] and the
 * text [正文] start at the top [顶格]; an attribute keeps to one column. A quote [引文] sinks {@value #QUOTE_SINK}
 * rows in every column it fills [低两格] and, unless it opens the text, leaves one blank column before it. A column
 * never opens on the closing punctuation of {@link #NO_COLUMN_START} [避头]: the glyph before it moves down with
 * it, and the column it leaves ends one row short.
 *
 * <p>A booklet with a figure keeps its text to the right half-page; the left one shows the figure's item as a
 * {@value #ICON}-pixel icon with the caption [图注] down one column below it, {@value #CAPTION_GAP} pixels apart,
 * the block centered both ways on the area.
 *
 * <p>⚠ No page turning: text past its half-page, or past one spread without a figure, refuses to lay out, naming
 * the paragraph.
 *
 * @author Alex
 * @version 1.0.0
 * @see BookletScreen
 * @since 1.0.0
 */

final class BookletLayout {

    static final int WIDTH = 272;
    static final int HEIGHT = 188;
    static final int RIGHT_X = 144;
    static final int LEFT_X = 16;
    static final int TOP = 24;
    static final int AREA_WIDTH = 112;
    static final int AREA_HEIGHT = 150;
    static final int COLUMNS = 8;
    static final int ROWS = 12;
    static final int PITCH = 14;
    static final int ROW_HEIGHT = 12;
    static final int GLYPH_X = 2;
    static final int GLYPH_Y = 3;
    static final int INK_CENTER = 5;
    static final int QUOTE_SINK = 2;
    static final int ICON = 48;
    static final int CAPTION_GAP = 12;
    static final String NO_COLUMN_START = "︐︑︒︓︔︕︖︙︶︸︺︼︾﹀﹂﹄﹈";
    private static final String BLANK = "　";

    private BookletLayout() {}

    static Spread lay(Booklet booklet, boolean traditional) {
        Plate plate = booklet.getFigure().map(figure -> plate(figure.caption(traditional))).orElse(null);
        return new Spread(layText(booklet.getParagraphs(), plate == null ? 2 * COLUMNS : COLUMNS, traditional), plate);
    }

    static List<Cell> layText(List<Booklet.Paragraph> paragraphs, int columns, boolean traditional) {
        List<Cell> cells = new ArrayList<>();
        int column = 0;
        for (Booklet.Paragraph paragraph : paragraphs) {
            String text = paragraph.glyphs(traditional);
            List<String> glyphs = split(text);
            int first = paragraph.role() == Booklet.Role.QUOTE ? QUOTE_SINK : 0;
            int rows = ROWS - first;
            if (paragraph.role() == Booklet.Role.ATTRIBUTE && glyphs.size() > rows) {
                throw new IllegalArgumentException(text + " overflows the one column of an attribute line");
            }
            if (opensNoColumn(glyphs.getFirst())) throw new IllegalArgumentException(text + " opens on punctuation");
            if (paragraph.role() == Booklet.Role.QUOTE && column > 0) column++;
            for (int start = 0; start < glyphs.size(); column++) {
                if (column == columns) throw new IllegalArgumentException(text + " overflows " + columns + " columns");
                int end = Math.min(start + rows, glyphs.size());
                while (end < glyphs.size() && end > start && opensNoColumn(glyphs.get(end))) end--;
                if (end == start) throw new IllegalArgumentException(text + " holds a column of punctuation");
                int x = getColumnX(column) + GLYPH_X;
                for (int at = start; at < end; at++) {
                    if (glyphs.get(at).equals(BLANK)) continue;
                    cells.add(new Cell(glyphs.get(at), x, getRowY(first + at - start)));
                }
                start = end;
            }
        }
        return List.copyOf(cells);
    }

    static Plate plate(String caption) {
        List<String> glyphs = split(caption);
        int height = ICON + CAPTION_GAP + glyphs.size() * ROW_HEIGHT;
        if (height > AREA_HEIGHT) throw new IllegalArgumentException(caption + " overflows the figure page");
        int top = TOP + (AREA_HEIGHT - height) / 2;
        int x = LEFT_X + AREA_WIDTH / 2 - INK_CENTER;
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < glyphs.size(); row++) {
            cells.add(new Cell(glyphs.get(row), x, top + ICON + CAPTION_GAP + row * ROW_HEIGHT));
        }
        return new Plate(LEFT_X + (AREA_WIDTH - ICON) / 2, top, List.copyOf(cells));
    }

    static int getColumnX(int column) {
        return column < COLUMNS
                ? RIGHT_X + (COLUMNS - 1 - column) * PITCH
                : LEFT_X + (2 * COLUMNS - 1 - column) * PITCH;
    }

    static int getRowY(int row) { return TOP + GLYPH_Y + row * ROW_HEIGHT; }

    private static boolean opensNoColumn(String glyph) { return NO_COLUMN_START.contains(glyph); }

    private static List<String> split(String text) {
        return text.codePoints().mapToObj(Character::toString).toList();
    }

    record Cell(String glyph, int x, int y) {}

    record Plate(int iconX, int iconY, List<Cell> caption) {}

    record Spread(List<Cell> text, @Nullable Plate plate) {}
}
