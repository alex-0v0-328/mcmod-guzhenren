package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.Chronicle;
import org.jetbrains.annotations.Nullable;

/**
 * {@link Booklet} 跨页排版引擎：仿照翻开的竖排线装书计算每个汉字的精确屏幕位置。
 * <ul>
 *   <li>阅读顺序遵循传统竖排：列内自上而下、列间自右向左，先排右半页（从最右列起），再排左半页。</li>
 *   <li>跨页精灵图尺寸为 {@value #WIDTH}×{@value #HEIGHT} 且不做缩放；每半页文字区为
 *       {@value #AREA_WIDTH}×{@value #AREA_HEIGHT}，左上角分别位于右页 ({@value #RIGHT_X},{@value #TOP}) 与左页
 *       ({@value #LEFT_X},{@value #TOP})，容纳 {@value #COLUMNS} 列 {@value #ROWS} 行，列距 {@value #PITCH} 像素、
 *       行高 {@value #ROW_HEIGHT} 像素。</li>
 *   <li>底图自带 1 像素界栏（x = 144+14k 与 16+14k，k 取 1 至 7）；书册字体每个字形为 12×12 格（墨迹占 0 至 10
 *       列），绘制在列起点向右 {@value #GLYPH_X} 像素、文字区顶加行偏移向下 {@value #GLYPH_Y} 像素处，使墨迹距两侧界栏
 *       以及最右列墨迹距版框均留出 1 像素空隙。</li>
 *   <li>每段另起一列，满列则自动接续下一列；正文与属性行顶格起排（属性行独占一列），引文每列下沉 {@value #QUOTE_SINK}
 *       行（低两格）且非开篇时前空一列；实行避头原则，列首禁止出现 {@link #NO_COLUMN_START} 中的收尾标点，遇之则连同
 *       前一字一起顺移至下一列，使原列少排一行。</li>
 *   <li>带有插图或属于蛊方（左半页画蛊方网格）时仅在右半页排版文字；左半页在文字区内居中绘制插图物品的
 *       {@value #ICON} 像素图标，图注在其下方相隔 {@value #CAPTION_GAP} 像素处竖排一列。</li>
 *   <li>古籍（{@link #layChronicle}）跨页连排、右开翻页：首列顶格卷端题，节题自次列起低 {@value #HEADING_SINK} 格，
 *       正文另起一列连排不分段；正文后空一列，顶格卷末题。除列首避开收尾括号外，列尾也不留开头括号
 *       （{@link Chronicle#OPENINGS}），遇之移到下一列。</li>
 *   <li>古籍的圈点（{@link Dot}，坐标为左上角）不占格，画在字格右下角，与字形墨迹略有重叠：句圈为 3×3 空心圈，读点
 *       为 2×2 实点。</li>
 *   <li>古籍每个跨页的两侧书口（底图外侧 9 像素宽的版心条）用 8 像素书口字体竖写：单鱼尾上书名，鱼尾下节次，下端汉
 *       字叶码；每个跨页一个叶码，两侧相同。</li>
 *   <li>⚠ 单页书册不支持翻页：文字超出半页（有插图时）或超出整个跨页（无插图时）会直接拒绝排版并点名报错对应段
 *       落。</li>
 * </ul>
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
    static final int HEADING_SINK = 2;
    static final int STOP_SIZE = 3;
    static final int PAUSE_SIZE = 2;
    static final int MARGIN_PITCH = 8;
    static final int LEFT_MARGIN_X = 6;
    static final int RIGHT_MARGIN_X = 259;
    static final int MARGIN_TOP = 24;
    static final int FISHTAIL_TOP = 56;
    static final int FISHTAIL_BOTTOM = 64;
    static final int MARGIN_BOTTOM = 174;
    static final int MARGIN_GAP = 4;
    private static final String BLANK = "　";

    private BookletLayout() {}

    static Spread lay(Booklet booklet, boolean traditional) {
        Plate plate = booklet.getFigure().map(figure -> plate(figure.caption(traditional))).orElse(null);
        boolean halfPage = plate != null || booklet.getRecipe().isPresent();
        return new Spread(layText(booklet.getParagraphs(), halfPage ? COLUMNS : 2 * COLUMNS, traditional), plate);
    }

    static List<Spread> layChronicle(Chronicle chronicle, boolean traditional) {
        List<List<Placed>> columns = new ArrayList<>();
        flow(tokens(chronicle.heading(traditional)), 0, columns);
        List<String> lines = chronicle.lines(traditional);
        flow(Chronicle.convert(lines.getFirst()), HEADING_SINK, columns);
        flow(Chronicle.read(lines), 0, columns);
        columns.add(List.of());
        flow(tokens(chronicle.colophon(traditional)), 0, columns);

        int spreads = (columns.size() + 2 * COLUMNS - 1) / (2 * COLUMNS);
        List<Spread> laid = new ArrayList<>();
        for (int leaf = 0; leaf < spreads; leaf++) {
            List<Cell> text = new ArrayList<>();
            List<Dot> dots = new ArrayList<>();
            for (int column = 0; column < 2 * COLUMNS; column++) {
                int at = leaf * 2 * COLUMNS + column;
                if (at >= columns.size()) break;
                int x = getColumnX(column) + GLYPH_X;
                for (Placed placed : columns.get(at)) {
                    int y = getRowY(placed.row());
                    text.add(new Cell(placed.token().glyph(), x, y));
                    if (placed.token().mark() != Chronicle.Mark.NONE) dots.add(dot(x, y, placed.token().mark()));
                }
            }
            laid.add(new Spread(List.copyOf(text), null, List.copyOf(dots), margin(chronicle, traditional, leaf + 1)));
        }
        return List.copyOf(laid);
    }

    private static void flow(List<Chronicle.Token> tokens, int sink, List<List<Placed>> columns) {
        int rows = ROWS - sink;
        for (int start = 0; start < tokens.size(); ) {
            int end = Math.min(start + rows, tokens.size());
            while (end < tokens.size() && end > start && closes(tokens.get(end).glyph())) end--;
            while (end < tokens.size() && end > start + 1 && Chronicle.OPENINGS.contains(tokens.get(end - 1).glyph())) {
                end--;
            }
            if (end == start) throw new IllegalArgumentException(tokens + " holds a column of punctuation");
            List<Placed> column = new ArrayList<>();
            for (int at = start; at < end; at++) column.add(new Placed(tokens.get(at), sink + at - start));
            columns.add(List.copyOf(column));
            start = end;
        }
    }

    static Dot dot(int x, int y, Chronicle.Mark mark) {
        return mark == Chronicle.Mark.STOP
                ? new Dot(x + 9, y + 10, STOP_SIZE, true)
                : new Dot(x + 10, y + 11, PAUSE_SIZE, false);
    }

    static List<Cell> margin(Chronicle chronicle, boolean traditional, int leaf) {
        List<String> title = split(chronicle.book().title(traditional));
        List<String> section = split(chronicle.margin(traditional));
        List<String> number = split(Chronicle.numeral(leaf));
        int titleTop = MARGIN_TOP + (FISHTAIL_TOP - MARGIN_TOP - title.size() * MARGIN_PITCH) / 2;
        int sectionTop = FISHTAIL_BOTTOM + MARGIN_GAP;
        int numberTop = MARGIN_BOTTOM - MARGIN_GAP - number.size() * MARGIN_PITCH;
        if (title.size() * MARGIN_PITCH > FISHTAIL_TOP - MARGIN_TOP) {
            throw new IllegalArgumentException(chronicle + " overflows the margin above the fishtail");
        }
        if (sectionTop + section.size() * MARGIN_PITCH + MARGIN_GAP > numberTop) {
            throw new IllegalArgumentException(chronicle + " overflows the margin at leaf " + leaf);
        }
        List<Cell> cells = new ArrayList<>();
        for (int x : new int[] { RIGHT_MARGIN_X, LEFT_MARGIN_X }) {
            stack(title, x, titleTop, cells);
            stack(section, x, sectionTop, cells);
            stack(number, x, numberTop, cells);
        }
        return List.copyOf(cells);
    }

    private static void stack(List<String> glyphs, int x, int top, List<Cell> cells) {
        for (int at = 0; at < glyphs.size(); at++) cells.add(new Cell(glyphs.get(at), x, top + at * MARGIN_PITCH));
    }

    private static List<Chronicle.Token> tokens(String text) {
        return split(text).stream().map(glyph -> new Chronicle.Token(glyph, Chronicle.Mark.NONE)).toList();
    }

    private static boolean closes(String glyph) {
        return NO_COLUMN_START.contains(glyph) || Chronicle.CLOSINGS.contains(glyph);
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

    record Dot(int x, int y, int size, boolean stop) {}

    private record Placed(Chronicle.Token token, int row) {}

    record Spread(List<Cell> text, @Nullable Plate plate, List<Dot> dots, List<Cell> margin) {

        Spread(List<Cell> text, @Nullable Plate plate) { this(text, plate, List.of(), List.of()); }
    }
}
