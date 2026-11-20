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
 *       留原文现代标点；排版时才经 {@link #read} 换成竖排直角括号与朱色圈点（句圈、读点），以后改回竖排标点只
 *       动代码。</li>
 *   <li>换算规则：「“”（）」→「﹁﹂」，「‘’《》」→「﹃﹄」，占格；「。！？…」为句圈、「，、；：—」为读点，附着在其前最
 *       近的汉字上（跳过括号），同字两标只留句圈；段首空格删去，没有汉字的段落删去；不分段，第二段起每段前加一个占格
 *       的「{@value #PARAGRAPH}」。</li>
 *   <li>卷端题「人祖传第一节」（{@link #heading}）、卷末题「人祖传第一节终」、书口节次「第一节」与物品名「「人祖传」第
 *       一节」都由 {@link #numeral}（1 至 99 的汉字数字）拼出；节题即书文首行。</li>
 *   <li>简繁文本由 OpenCC {@code s2tw} 一次性转换后入库，每行码点数必须相等（{@code ChronicleTextTest}），以保证两
 *       套版式一致。</li>
 *   <li>⚠ 书文或本类字面量新增字形后须重跑 {@code tools/booklet_font.py}，否则 {@code BookletFontTest} 失败。</li>
 * </ul>
 */

public record Chronicle(Book book, int section) implements BookletText {

    public static final String PARAGRAPH = "○";
    public static final String OPENINGS = "﹁﹃";
    public static final String CLOSINGS = "﹂﹄";
    private static final String NUMERALS = "一二三四五六七八九";
    private static final String TEN = "十";
    private static final String ORDINAL = "第";
    private static final String STOPS = "。！？…";
    private static final String PAUSES = "，、；：—";
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

    public String margin(boolean traditional) {
        return ORDINAL + numeral(section) + book.unit(traditional);
    }

    public String itemName(boolean traditional) { return "「" + book.title(traditional) + "」" + margin(traditional); }

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

    public List<Token> read(boolean traditional) { return read(lines(traditional)); }

    public static List<Token> read(List<String> lines) {
        List<Token> tokens = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<Token> paragraph = convert(line);
            if (paragraph.stream().noneMatch(Token::isHan)) continue;
            if (!tokens.isEmpty()) tokens.add(new Token(PARAGRAPH, Mark.NONE));
            tokens.addAll(paragraph);
        }
        return List.copyOf(tokens);
    }

    public static List<Token> convert(String line) {
        List<Token> tokens = new ArrayList<>();
        line.codePoints().forEach(codePoint -> {
            String glyph = Character.toString(codePoint);
            if (QUOTE_OPEN.contains(glyph)) tokens.add(new Token("﹁", Mark.NONE));
            else if (QUOTE_CLOSE.contains(glyph)) tokens.add(new Token("﹂", Mark.NONE));
            else if (INNER_OPEN.contains(glyph)) tokens.add(new Token("﹃", Mark.NONE));
            else if (INNER_CLOSE.contains(glyph)) tokens.add(new Token("﹄", Mark.NONE));
            else if (STOPS.contains(glyph)) mark(tokens, Mark.STOP);
            else if (PAUSES.contains(glyph)) mark(tokens, Mark.PAUSE);
            else if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                tokens.add(new Token(glyph, Mark.NONE));
            } else if (!Character.isWhitespace(codePoint) && codePoint != 0x3000) {
                throw new IllegalArgumentException("unexpected " + glyph + " in " + line);
            }
        });
        return tokens;
    }

    private static void mark(List<Token> tokens, Mark mark) {
        for (int at = tokens.size() - 1; at >= 0; at--) {
            Token token = tokens.get(at);
            if (!token.isHan()) continue;
            if (token.mark().ordinal() < mark.ordinal()) tokens.set(at, new Token(token.glyph(), mark));
            return;
        }
    }

    public static String numeral(int number) {
        if (number < 1 || number > 99) throw new IllegalArgumentException("numeral " + number);
        int tens = number / 10;
        int ones = number % 10;
        String head = tens == 0 ? "" : (tens == 1 ? "" : NUMERALS.substring(tens - 1, tens)) + TEN;
        return head + (ones == 0 ? "" : NUMERALS.substring(ones - 1, ones));
    }

    public enum Mark { NONE, PAUSE, STOP }

    public record Token(String glyph, Mark mark) {

        public boolean isHan() {
            return Character.UnicodeScript.of(glyph.codePointAt(0)) == Character.UnicodeScript.HAN;
        }
    }

    public enum Book {
        RENZU_ZHUAN("renzu_zhuan", 38, "人祖传", "人祖傳", "Legend of the Human Ancestor");

        private final String id;
        private final int sections;
        private final String simplified;
        private final String traditional;
        private final String english;

        Book(String id, int sections, String simplified, String traditional, String english) {
            this.id = id;
            this.sections = sections;
            this.simplified = simplified;
            this.traditional = traditional;
            this.english = english;
        }

        public String id() { return id; }

        public int sections() { return sections; }

        public String english() { return english; }

        public String title(boolean traditional) { return traditional ? this.traditional : simplified; }

        String unit(boolean traditional) { return traditional ? "節" : "节"; }
    }
}
