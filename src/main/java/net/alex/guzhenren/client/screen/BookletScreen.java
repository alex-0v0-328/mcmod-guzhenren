package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
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
 *   <li>蛊方图册（{@link #openRecipe}）右半页排该蛊方的书文（数据包蛊方没有书文则留空），左半页按 {@link GuRecipe} 画
 *       5×5 缺角网格：每组材料画在组内首格并标数量，可放该材料的其余格浅色打底；网格下方是产物图标和名字，悬停出物
 *       品提示。</li>
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
    private static final int GRID_PITCH = 20;
    private static final int GRID_CELL = 18;
    private static final int GRID_X =
            BookletLayout.LEFT_X + (BookletLayout.AREA_WIDTH - GuRecipe.GRID_COLS * GRID_PITCH) / 2;
    private static final int GRID_Y = BookletLayout.TOP + 8;
    private static final int RESULT_Y = GRID_Y + GuRecipe.GRID_ROWS * GRID_PITCH + 10;
    private static final int CELL_RULE = 0x802F2925;
    private static final int GROUP_TINT = 0x262F2925;
    private final List<Glyph> glyphs = new ArrayList<>();
    private final @Nullable BookletLayout.Plate plate;
    private final ItemStack icon;
    private final @Nullable GuRecipe recipe;
    private int leftPos;
    private int topPos;

    private BookletScreen(Component title, BookletLayout.Spread spread, ItemStack icon, ResourceLocation font,
                          @Nullable GuRecipe recipe) {
        super(title);
        this.plate = spread.plate();
        this.icon = icon;
        this.recipe = recipe;
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
                traditional ? HANT : HANS, null));
    }

    public static void openRecipe(ResourceLocation id) {
        GuRecipe recipe = KnownRecipes.find(id).orElse(null);
        if (recipe == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean traditional = TRADITIONAL.equals(minecraft.getLanguageManager().getSelected());
        BookletLayout.Spread spread = Booklet.forRecipe(id).map(booklet -> BookletLayout.lay(booklet, traditional))
                .orElse(new BookletLayout.Spread(List.of(), null));
        ItemStack result = RecipeGhosts.getResult(recipe);
        minecraft.setScreen(new BookletScreen(result.getHoverName(), spread, ItemStack.EMPTY,
                traditional ? HANT : HANS, recipe));
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
        if (recipe != null) renderRecipe(graphics, recipe, mouseX, mouseY);
        GuiBatch.draw(graphics, () -> {
            for (Glyph glyph : glyphs) {
                graphics.drawString(font, glyph.text(), leftPos + glyph.x(), topPos + glyph.y(), INK, false);
            }
        });
    }

    //region the recipe grid on the left half-page [左半页蛊方网格]
    private void renderRecipe(GuiGraphics graphics, GuRecipe shown, int mouseX, int mouseY) {
        int x = leftPos + BookletLayout.LEFT_X;
        int y = topPos + BookletLayout.TOP;
        graphics.fill(x, y, x + BookletLayout.AREA_WIDTH, y + BookletLayout.AREA_HEIGHT, PAPER);

        ItemStack hovered = ItemStack.EMPTY;
        for (int row = 0; row < GuRecipe.GRID_ROWS; row++) {
            for (int col = 0; col < GuRecipe.GRID_COLS; col++) {
                if (GuRecipe.getSlotAt(row, col) < 0) continue;
                graphics.renderOutline(getCellX(col), getCellY(row), GRID_CELL, GRID_CELL, CELL_RULE);
            }
        }
        for (int n = 0; n < shown.ingredients().size(); n++) {
            GuIngredient need = shown.ingredients().get(n);
            List<Integer> cells = shown.slots().get(n);
            ItemStack stack = RecipeGhosts.getOption(need).copyWithCount(need.count());
            for (int i = 0; i < cells.size(); i++) {
                int cellX = getCellX(getCol(cells.get(i)));
                int cellY = getCellY(getRow(cells.get(i)));
                if (i == 0) {
                    graphics.renderItem(stack, cellX + 1, cellY + 1);
                    graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);
                } else {
                    graphics.fill(cellX + 1, cellY + 1, cellX + GRID_CELL - 1, cellY + GRID_CELL - 1, GROUP_TINT);
                }
                if (isOver(mouseX, mouseY, cellX, cellY, GRID_CELL, GRID_CELL)) hovered = stack;
            }
        }

        ItemStack result = RecipeGhosts.getResult(shown);
        int room = BookletLayout.AREA_WIDTH - 2 - 16 - 4;
        FormattedCharSequence name = Language.getInstance().getVisualOrder(
                font.substrByWidth(result.getHoverName(), room));
        int width = 16 + 4 + font.width(name);
        int resultX = x + (BookletLayout.AREA_WIDTH - width) / 2;
        int resultY = topPos + RESULT_Y;
        graphics.renderItem(result, resultX, resultY);
        graphics.drawString(font, name, resultX + 20, resultY + (16 - font.lineHeight) / 2 + 1, INK, false);
        if (isOver(mouseX, mouseY, resultX, resultY, 16, 16)) hovered = result;

        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    private int getCellX(int col) { return leftPos + GRID_X + col * GRID_PITCH; }

    private int getCellY(int row) { return topPos + GRID_Y + row * GRID_PITCH; }

    private static int getCol(int slot) {
        return slot < GuRecipe.RING_SIZE ? GuRecipe.getRingCol(slot)
                : (slot - GuRecipe.RING_SIZE) % GuRecipe.CORE_COLS + 1;
    }

    private static int getRow(int slot) {
        return slot < GuRecipe.RING_SIZE ? GuRecipe.getRingRow(slot)
                : (slot - GuRecipe.RING_SIZE) / GuRecipe.CORE_COLS + 1;
    }

    private static boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
    //endregion

    private record Glyph(FormattedCharSequence text, int x, int y) {}
}
