package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.item.Booklet;
import org.jetbrains.annotations.Nullable;

/**
 * {@link Booklet} 跨页上每个字的位置，仿翻开的线装书。
 * <ul>
 *   <li>字序：列内自上而下，列间自右向左；先右半页（从最右列起），再左半页。</li>
 *   <li>跨页 sprite {@value #WIDTH}x{@value #HEIGHT} 不缩放；每半页文字区 {@value #AREA_WIDTH}x{@value #AREA_HEIGHT}，
 *       左上角右页 ({@value #RIGHT_X},{@value #TOP})、左页 ({@value #LEFT_X},{@value #TOP})；{@value #COLUMNS} 列
 *       {@value #ROWS} 行，列距 {@value #PITCH}、行高 {@value #ROW_HEIGHT} 像素。</li>
 *   <li>sprite 带 1 像素界栏，x = 144+14k、16+14k，k 取 1 至 7；字为书册字体 12x12 格，墨占 0 至 10 列，画在列起点右
 *       {@value #GLYPH_X}、文字区顶加行偏移下 {@value #GLYPH_Y} 像素；墨距两侧界栏、最右列墨距版框各 1 像素。</li>
 *   <li>每段另起一列，满则接下列；正文、属性行顶格，属性行只占一列；引文每列下沉 {@value #QUOTE_SINK} 行（低两格），
 *       非开篇则前空一列；避头：列首不放 {@link #NO_COLUMN_START} 中的收尾标点，前一字随之下移，原列少一行。</li>
 *   <li>有插图时只在右半页排文字；左半页画插图物品的 {@value #ICON} 像素图标，图注在其下竖排一列、相隔
 *       {@value #CAPTION_GAP} 像素，整块在文字区内居中。</li>
 *   <li>⚠ 不翻页：文字超出半页，或无插图时超出一个跨页，拒绝排版并报出该段。</li>
 * </ul>
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
