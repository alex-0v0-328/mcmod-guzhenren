package net.alex.guzhenren.item;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 书册（{@link BookletItem}）的书文定义枚举：每本书册对应一个枚举项，每个段落同时维护简体与繁体两套文本。
 * <ul>
 *   <li>封闭词表：每本书册仅包含自上而下、自右向左排版的段落列表（{@link Paragraph}），以及左半页可选的一幅插图（
 *       {@link Figure}）。</li>
 *   <li>蛊方书文（{@code RECIPE_*}）不是书册物品，由蛊方物品按配方 ID（{@link #forRecipe}）取用：只写转数、道与要哪些蛊
 *       和蛊材，不写摆位，摆位由左半页的网格图画出；不用标点，「需要蛊虫」「配合蛊材」各占一列作题，其下每项一列、
 *       低一格。</li>
 *   <li>书文不走语言文件：仅在本枚举内以中文维护一份，任何客户端语言环境下均渲染为中文（{@code zh_tw} 使用繁体，其余语
 *       言使用简体）；构建脚本 {@code tools/booklet_font.py} 直接从本文件的字符串字面量中提取字形生成位图字体。</li>
 *   <li>同一段落的简繁文本码点数必须严格相等（{@link #requireTwins}），以保证版式完全一致；标点采用竖排专有字符
 *       （{@code ︐}、{@code ︒}）且各占一格，段首缩进使用全角空格 {@code U+3000}（占格而不绘制）。</li>
 *   <li>{@link Figure} 按注册 ID 指定所绘蛊虫并声明其转数与流派作为防漂移护栏：若与实际注册的蛊虫不符则
 *       {@code BookletGameTests} 失败，若属性段或图注与语言文件译名不符则 {@code BookletTextTest} 失败。</li>
 *   <li>⚠ 新增字形后必须重新运行 {@code tools/booklet_font.py} 更新位图字体，否则 {@code BookletFontTest} 会持续失
 *       败。</li>
 * </ul>
 */

public enum Booklet implements BookletText {

    GU_ATLAS_HOPE(new Figure("hope_gu", Rank.ONE, GuPath.HUMAN, "希望蛊", "希望蠱"),
            new Paragraph(Role.ATTRIBUTE, "一转　人道　蛊虫", "一轉　人道　蠱蟲"),
            new Paragraph(Role.TEXT, "希望蛊只是一个小小的微弱光点︒你需要它为你开窍︐让你走上蛊师的修行之路︒",
                    "希望蠱只是一個小小的微弱光點︒你需要它為你開竅︐讓你走上蠱師的修行之路︒"),
            new Paragraph(Role.QUOTE, "人啊︐你只要把心交给我︐就可以了︒", "人啊︐你只要把心交給我︐就可以了︒")),

    //region Gu recipes [蛊方]
    RECIPE_FOUR_FLAVORS_LIQUOR_WORM("refinement/four_flavors_liquor_worm_2",
            new Paragraph(Role.ATTRIBUTE, "二转　食道蛊虫　蛊方", "二轉　食道蠱蟲　蠱方"),
            new Paragraph(Role.TEXT, "需要蛊虫", "需要蠱蟲"),
            new Paragraph(Role.TEXT, "　一转酒虫两只", "　一轉酒蟲兩隻"),
            new Paragraph(Role.TEXT, "配合蛊材", "配合蠱材"),
            new Paragraph(Role.TEXT, "　酸酒一份", "　酸酒一份"),
            new Paragraph(Role.TEXT, "　甜酒一份", "　甜酒一份"),
            new Paragraph(Role.TEXT, "　苦酒一份", "　苦酒一份"),
            new Paragraph(Role.TEXT, "　辣酒一份", "　辣酒一份")),
    RECIPE_WHITE_JADE("refinement/white_jade_gu_2",
            new Paragraph(Role.ATTRIBUTE, "二转　土道蛊虫　蛊方", "二轉　土道蠱蟲　蠱方"),
            new Paragraph(Role.TEXT, "需要蛊虫", "需要蠱蟲"),
            new Paragraph(Role.TEXT, "　一转白豕蛊一只", "　一轉白豕蠱一隻"),
            new Paragraph(Role.TEXT, "　一转玉皮蛊一只", "　一轉玉皮蠱一隻")),
    RECIPE_MOONGLOW("refinement/moonglow_gu_2",
            new Paragraph(Role.ATTRIBUTE, "二转　月道蛊虫　蛊方", "二轉　月道蠱蟲　蠱方"),
            new Paragraph(Role.TEXT, "需要蛊虫", "需要蠱蟲"),
            new Paragraph(Role.TEXT, "　一转月光蛊一只", "　一轉月光蠱一隻"),
            new Paragraph(Role.TEXT, "　一转小光蛊两只", "　一轉小光蠱兩隻")),
    RECIPE_ALL_OUT_EFFORT("refinement/all_out_effort_gu_4",
            new Paragraph(Role.ATTRIBUTE, "四转　力道蛊虫　蛊方", "四轉　力道蠱蟲　蠱方"),
            new Paragraph(Role.TEXT, "需要蛊虫", "需要蠱蟲"),
            new Paragraph(Role.TEXT, "　三转全力以赴蛊一只", "　三轉全力以赴蠱一隻"),
            new Paragraph(Role.TEXT, "配合蛊材", "配合蠱材"),
            new Paragraph(Role.TEXT, "　铁块十六块", "　鐵塊十六塊"),
            new Paragraph(Role.TEXT, "　金块八块", "　金塊八塊")),
    RECIPE_HABITUAL_STRENGTH("refinement/habitual_strength_gu_4",
            new Paragraph(Role.ATTRIBUTE, "四转　力道蛊虫　蛊方", "四轉　力道蠱蟲　蠱方"),
            new Paragraph(Role.TEXT, "需要蛊虫", "需要蠱蟲"),
            new Paragraph(Role.TEXT, "　三转蓄力蛊一只", "　三轉蓄力蠱一隻"),
            new Paragraph(Role.TEXT, "配合蛊材", "配合蠱材"),
            new Paragraph(Role.TEXT, "　兔子腿四只", "　兔子腿四隻"));
    //endregion

    private final @Nullable Figure figure;
    private final @Nullable String recipe;
    private final List<Paragraph> paragraphs;

    Booklet(@Nullable Figure figure, Paragraph... paragraphs) {
        this.figure = figure;
        this.recipe = null;
        this.paragraphs = List.of(paragraphs);
    }

    Booklet(String recipe, Paragraph... paragraphs) {
        this.figure = null;
        this.recipe = recipe;
        this.paragraphs = List.of(paragraphs);
    }

    public static Optional<Booklet> forRecipe(ResourceLocation id) {
        return Arrays.stream(values())
                .filter(booklet -> booklet.getRecipe().filter(id::equals).isPresent())
                .findFirst();
    }

    public Optional<Figure> getFigure() { return Optional.ofNullable(figure); }

    /** 蛊方书文对应的炼蛊配方 ID；有它的书文只排右半页，左半页由蛊方图册画网格。 */
    public Optional<ResourceLocation> getRecipe() { return Optional.ofNullable(recipe).map(Guzhenren::id); }

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
