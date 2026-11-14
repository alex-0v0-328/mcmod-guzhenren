package net.alex.guzhenren.item;

import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 书册的书文：每本一个常量，每段带简繁两套字。
 * <ul>
 *   <li>封闭词表：一本书只有自上而下、自右向左的段落，加左半页可选的一幅图。</li>
 *   <li>书文不进语言文件：只用中文写一次，各语言都显示中文（{@code zh_tw} 繁体，其余简体）；
 *       {@code tools/booklet_font.py} 从本文件字符串字面量截取位图字体。</li>
 *   <li>每段简繁字数相同，排版一致；标点用竖排（︐ ︒）各占一格，全角空格 {@code U+3000} 占格不画。</li>
 *   <li>图按注册 ID 指明所画的蛊，并声明其转数与道[Path]，作防漂移护栏：注册的蛊不符时 {@code BookletGameTests} 失败，
 *       属性行或图注与语言文件不符时 {@code BookletTextTest} 失败。</li>
 *   <li>⚠ 新增的字须重跑 {@code tools/booklet_font.py} 才发布，此前 {@code BookletFontTest} 一直失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BookletItem
 * @since 1.0.0
 */

public enum Booklet {

    GU_ATLAS_HOPE(new Figure("hope_gu", Rank.ONE, GuPath.HUMAN, "希望蛊", "希望蠱"),
            new Paragraph(Role.ATTRIBUTE, "一转　人道　蛊虫", "一轉　人道　蠱蟲"),
            new Paragraph(Role.TEXT, "希望蛊只是一个小小的微弱光点︒你需要它为你开窍︐让你走上蛊师的修行之路︒",
                    "希望蠱只是一個小小的微弱光點︒你需要它為你開竅︐讓你走上蠱師的修行之路︒"),
            new Paragraph(Role.QUOTE, "人啊︐你只要把心交给我︐就可以了︒", "人啊︐你只要把心交給我︐就可以了︒"));

    private final @Nullable Figure figure;
    private final List<Paragraph> paragraphs;

    Booklet(@Nullable Figure figure, Paragraph... paragraphs) {
        this.figure = figure;
        this.paragraphs = List.of(paragraphs);
    }

    public Optional<Figure> getFigure() { return Optional.ofNullable(figure); }

    public List<Paragraph> getParagraphs() { return paragraphs; }

    public enum Role { ATTRIBUTE, TEXT, QUOTE }

    public record Paragraph(Role role, String simplified, String traditional) {

        public Paragraph {
            requireTwins(simplified, traditional);
        }

        public String glyphs(boolean traditional) { return traditional ? this.traditional : simplified; }
    }

    public record Figure(String id, Rank rank, GuPath path, String simplified, String traditional) {

        public Figure {
            requireTwins(simplified, traditional);
        }

        public String caption(boolean traditional) { return traditional ? this.traditional : simplified; }

        public Item getItem() { return BuiltInRegistries.ITEM.get(Guzhenren.id(id)); }
    }

    private static void requireTwins(String simplified, String traditional) {
        if (simplified.isEmpty()) throw new IllegalArgumentException("empty text");
        if (simplified.codePointCount(0, simplified.length()) != traditional.codePointCount(0, traditional.length())) {
            throw new IllegalArgumentException(simplified + " and " + traditional + " differ in length");
        }
    }
}
