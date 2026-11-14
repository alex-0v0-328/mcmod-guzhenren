package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 书册阅读界面。
 * <ul>
 *   <li>{@code book_spread} sprite 居中不缩放，自动 GUI 缩放总留出 320x240 区域，正好放下、不滚动；文字按
 *       {@link BookletLayout} 排，版框墨色 {@code #2f2925}，无阴影。</li>
 *   <li>打开时选定字体：{@code zh_tw} 用繁体文本和 {@code booklet_hant}，其他语言用简体和 {@code booklet_hans}；
 *       两套均由 {@code tools/booklet_font.py} 按 1 字形像素对 1 GUI 像素切出。</li>
 *   <li>插图页先用文字区底色 {@code #e0d0a5} 盖住界栏，再按格子三倍尺寸画物品。</li>
 *   <li>{@link #open} 客户端初始化时注册，供 {@link BookletItem} 用；Esc 关、单人暂停、旁白以书名为题，同原版。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BookletLayout
 * @since 1.0.0
 */

public final class BookletScreen extends Screen {

    private static final ResourceLocation SPREAD = Guzhenren.id("book_spread");
    private static final ResourceLocation HANS = Guzhenren.id("booklet_hans");
    private static final ResourceLocation HANT = Guzhenren.id("booklet_hant");
    private static final String TRADITIONAL = "zh_tw";
    private static final int INK = 0xFF2F2925;
    private static final int PAPER = 0xFFE0D0A5;
    private static final float ICON_SCALE = BookletLayout.ICON / 16.0F;
    private final List<Glyph> glyphs = new ArrayList<>();
    private final @Nullable BookletLayout.Plate plate;
    private final ItemStack icon;
    private int leftPos;
    private int topPos;

    private BookletScreen(Component title, BookletLayout.Spread spread, ItemStack icon, ResourceLocation font) {
        super(title);
        this.plate = spread.plate();
        this.icon = icon;
        List<BookletLayout.Cell> cells = new ArrayList<>(spread.text());
        if (plate != null) cells.addAll(plate.caption());
        for (BookletLayout.Cell cell : cells) {
            glyphs.add(new Glyph(Component.literal(cell.glyph()).withStyle(style -> style.withFont(font))
                    .getVisualOrderText(), cell.x(), cell.y()));
        }
    }

    public static void open(BookletItem item) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean traditional = TRADITIONAL.equals(minecraft.getLanguageManager().getSelected());
        Booklet booklet = item.getBooklet();
        ItemStack icon = booklet.getFigure().map(figure -> new ItemStack(figure.getItem())).orElse(ItemStack.EMPTY);
        minecraft.setScreen(new BookletScreen(item.getDescription(), BookletLayout.lay(booklet, traditional), icon,
                traditional ? HANT : HANS));
    }

    @Override
    protected void init() {
        leftPos = (width - BookletLayout.WIDTH) / 2;
        topPos = (height - BookletLayout.HEIGHT) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        SpriteBatch.draw(graphics, sprites ->
                sprites.blit(SPREAD, leftPos, topPos, BookletLayout.WIDTH, BookletLayout.HEIGHT));
        if (plate != null) {
            int x = leftPos + BookletLayout.LEFT_X;
            int y = topPos + BookletLayout.TOP;
            graphics.fill(x, y, x + BookletLayout.AREA_WIDTH, y + BookletLayout.AREA_HEIGHT, PAPER);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(leftPos + plate.iconX(), topPos + plate.iconY(), 0.0F);
            pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            graphics.renderItem(icon, 0, 0);
            pose.popPose();
        }
        GuiBatch.draw(graphics, () -> {
            for (Glyph glyph : glyphs) {
                graphics.drawString(font, glyph.text(), leftPos + glyph.x(), topPos + glyph.y(), INK, false);
            }
        });
    }

    private record Glyph(FormattedCharSequence text, int x, int y) {}
}
