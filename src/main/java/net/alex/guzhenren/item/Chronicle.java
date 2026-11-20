package net.alex.guzhenren.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 古籍书册（{@link BookletItem}）的书文：一部古籍分节成册，每册一节，书文按节存为资源文本而不写进代码。
 * <ul>
 *   <li>书文位于 {@code assets/guzhenren/booklets/<书>/zh_hans|zh_hant/<两位节号>.txt}：首行节题，其后每行一段，保
 *       留原文横排标点；排版时才经 {@link #read} 换成竖排标点，换法只动代码。</li>
 *   <li>换算参照台湾竖排书：每个标点占一格，「。，、；：！？…—」换成对应竖排字形（「……」「——」各占两格）；「“”（）」→
 *       「﹁﹂」，「‘’《》」→「﹃﹄」（Alex 定统一用直角括号）；段首空格删去，空段删去，每段单独成段。</li>
 *   <li>卷端题「人祖传第一节」（{@link #heading}）、卷末题「人祖传第一节终」与物品名「「人祖传」第一节」都由
 *       {@link #numeral}（1 至 99 的汉字数字）拼出；节题即书文首行。</li>
 *   <li>简繁文本由 OpenCC {@code s2tw} 一次性转换后入库，每行码点数必须相等（{@code ChronicleTextTest}），以保证两
 *       套版式一致。</li>
 *   <li>⚠ 书文或本类字面量新增字形后须重跑 {@code tools/booklet_font.py}，否则 {@code BookletFontTest} 失败。</li>
 * </ul>
 */

public record Chronicle(Book book, int section) implements BookletText {

    public static final String OPENINGS = "﹁﹃";
    public static final String CLOSINGS = "﹂﹄";
    public static final String DASH = "︱";
    private static final String NUMERALS = "一二三四五六七八九";
    private static final String TEN = "十";
    private static final String ORDINAL = "第";
    private static final String HORIZONTAL = "。，、；：！？…—";
    private static final String VERTICAL = "︒︐︑︔︓︕︖︙︱";
    private static final String QUOTE_OPEN = "“（";
    private static final String QUOTE_CLOSE = "”）";
    private static final String INNER_OPEN = "‘《";
    private static final String INNER_CLOSE = "’》";

    public Chronicle {
        if (section < 1 || section > book.sections()) {
            throw new IllegalArgumentException(book + " has no section " + section);
        }
    }

    public static List<Chronicle> sections(Book book) {
        List<Chronicle> sections = new ArrayList<>();
        for (int section = 1; section <= book.sections(); section++) sections.add(new Chronicle(book, section));
        return List.copyOf(sections);
    }

    public String heading(boolean traditional) {
        return book.title(traditional) + ORDINAL + numeral(section) + book.unit(traditional);
    }

    public String colophon(boolean traditional) { return heading(traditional) + (traditional ? "終" : "终"); }

    public String numbered(boolean traditional) {
        return ORDINAL + numeral(section) + book.unit(traditional);
    }

    public String itemName(boolean traditional) { return "「" + book.title(traditional) + "」" + numbered(traditional); }

    public List<String> lines(boolean traditional) {
        String path = "/assets/guzhenren/booklets/" + book.id() + "/" + (traditional ? "zh_hant" : "zh_hans") + "/"
                + String.format(Locale.ROOT, "%02d", section) + ".txt";
        try (InputStream input = Chronicle.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing " + path);
            return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .filter(line -> !line.isBlank()).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public String title(boolean traditional) { return lines(traditional).getFirst(); }

    public List<List<String>> read(boolean traditional) { return read(lines(traditional)); }

    public static List<List<String>> read(List<String> lines) {
        return lines.subList(1, lines.size()).stream().map(Chronicle::convert).filter(glyphs -> !glyphs.isEmpty())
                .toList();
    }

    public static List<String> convert(String line) {
        List<String> glyphs = new ArrayList<>();
        line.codePoints().forEach(codePoint -> {
            String glyph = Character.toString(codePoint);
            int punctuation = HORIZONTAL.indexOf(glyph);
            if (QUOTE_OPEN.contains(glyph)) glyphs.add("﹁");
            else if (QUOTE_CLOSE.contains(glyph)) glyphs.add("﹂");
            else if (INNER_OPEN.contains(glyph)) glyphs.add("﹃");
            else if (INNER_CLOSE.contains(glyph)) glyphs.add("﹄");
            else if (punctuation >= 0) glyphs.add(VERTICAL.substring(punctuation, punctuation + 1));
            else if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) glyphs.add(glyph);
            else if (!Character.isWhitespace(codePoint) && codePoint != 0x3000) {
                throw new IllegalArgumentException("unexpected " + glyph + " in " + line);
            }
        });
        return List.copyOf(glyphs);
    }

    public static String numeral(int number) {
        if (number < 1 || number > 99) throw new IllegalArgumentException("numeral " + number);
        int tens = number / 10;
        int ones = number % 10;
        String head = tens == 0 ? "" : (tens == 1 ? "" : NUMERALS.substring(tens - 1, tens)) + TEN;
        return head + (ones == 0 ? "" : NUMERALS.substring(ones - 1, ones));
    }

    public enum Book {
        RENZU_ZHUAN("renzu_zhuan", 38, "人祖传", "人祖傳");

        private final String id;
        private final int sections;
        private final String simplified;
        private final String traditional;

        Book(String id, int sections, String simplified, String traditional) {
            this.id = id;
            this.sections = sections;
            this.simplified = simplified;
            this.traditional = traditional;
        }

        public String id() { return id; }

        public int sections() { return sections; }

        public String title(boolean traditional) { return traditional ? this.traditional : simplified; }

        String unit(boolean traditional) { return traditional ? "節" : "节"; }
    }
}
