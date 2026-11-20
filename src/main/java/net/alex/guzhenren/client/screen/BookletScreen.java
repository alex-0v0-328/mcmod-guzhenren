package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.Chronicle;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 线装书册阅读界面。
 * <ul>
 *   <li>{@code book_spread} 精灵图居中且不做缩放，在自动 GUI 缩放下总能留出 320×240 区域，恰好完整展示而无需滚
 *       动；文字依据 {@link BookletLayout} 排版，使用无阴影的版框墨色 {@code #2f2925}。</li>
 *   <li>界面打开时确定字体与文本变体：{@code zh_tw} 语言环境使用繁体文本与 {@code booklet_hant} 字体，其余语言统一使用
 *       简体文本与 {@code booklet_hans} 字体；两套位图字体均由 {@code tools/booklet_font.py} 按 1 字形像素对应 1 GUI
 *       像素切出。</li>
 *   <li>带有插图的书册会先用文字区底色 {@code #e0d0a5} 遮盖左半页界栏，再按原版格子三倍尺寸居中绘制插图物品。</li>
 *   <li>蛊方图册（{@link #openRecipe}）右半页排该蛊方的书文（数据包蛊方没有书文则留空），左半页按 {@link GuRecipe} 画
 *       5×5 缺角网格：每组材料画在组内首格并标数量，可放该材料的其余格浅色打底；网格下方是产物图标和名字，悬停出物
 *       品提示。</li>
 *   <li>古籍（{@link Chronicle}）有多个跨页，右开：左侧箭头、← 键、PageDown、滚轮向下翻到下一页，右侧箭头、→ 键、
 *       PageUp、滚轮向上翻回；每次打开从首页读。书口字用 8 像素的 {@code booklet_margin_*} 字体，圈点以朱色
 *       {@code #c23b22} 直接填像素。</li>
 *   <li>翻页都播翻书声（按钮自身不播，免得点击时响两次）。</li>
 *   <li>⚠ 多页时方向键与 PageUp、PageDown 一律在此吞掉、先于原版焦点导航，到首末页也不放行，否则焦点会落到翻页按钮
 *       上，回车即误翻。</li>
 *   <li>{@link #open} 在客户端初始化时注册回调给 {@link BookletItem} 使用；交互行为沿用原版书本（Esc 关闭、单人暂
 *       停、旁白播报书名）。</li>
 * </ul>
 */

public final class BookletScreen extends Screen {

    private static final ResourceLocation SPREAD = Guzhenren.id("book_spread");
    private static final ResourceLocation HANS = Guzhenren.id("booklet_hans");
    private static final ResourceLocation HANT = Guzhenren.id("booklet_hant");
    private static final ResourceLocation MARGIN_HANS = Guzhenren.id("booklet_margin_hans");
    private static final ResourceLocation MARGIN_HANT = Guzhenren.id("booklet_margin_hant");
    private static final String TRADITIONAL = "zh_tw";
    private static final int INK = 0xFF2F2925;
    private static final int PAPER = 0xFFE0D0A5;
    private static final int VERMILION = 0xFFC23B22;
    private static final int TURN_WIDTH = 23;
    private static final int TURN_LIFT = 20;
    private static final float ICON_SCALE = BookletLayout.ICON / 16.0F;
    private static final int GRID_PITCH = 20;
    private static final int GRID_CELL = 18;
    private static final int GRID_X =
            BookletLayout.LEFT_X + (BookletLayout.AREA_WIDTH - GuRecipe.GRID_COLS * GRID_PITCH) / 2;
    private static final int GRID_Y = BookletLayout.TOP + 8;
    private static final int RESULT_Y = GRID_Y + GuRecipe.GRID_ROWS * GRID_PITCH + 10;
    private static final int CELL_RULE = 0x802F2925;
    private static final int GROUP_TINT = 0x262F2925;
    private final List<Page> pages = new ArrayList<>();
    private final ItemStack icon;
    private final @Nullable GuRecipe recipe;
    private int page;
    private int leftPos;
    private int topPos;
    private @Nullable PageButton next;
    private @Nullable PageButton previous;

    private BookletScreen(Component title, List<BookletLayout.Spread> spreads, ItemStack icon, boolean traditional,
                          @Nullable GuRecipe recipe) {
        super(title);
        this.icon = icon;
        this.recipe = recipe;
        ResourceLocation font = traditional ? HANT : HANS;
        ResourceLocation marginFont = traditional ? MARGIN_HANT : MARGIN_HANS;
        for (BookletLayout.Spread spread : spreads) {
            List<BookletLayout.Cell> cells = new ArrayList<>(spread.text());
            if (spread.plate() != null) cells.addAll(spread.plate().caption());
            List<Glyph> glyphs = new ArrayList<>(glyphs(cells, font));
            glyphs.addAll(glyphs(spread.margin(), marginFont));
            pages.add(new Page(List.copyOf(glyphs), spread.dots(), spread.plate()));
        }
    }

    public static void open(BookletItem item) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean traditional = TRADITIONAL.equals(minecraft.getLanguageManager().getSelected());
        BookletScreen screen = switch (item.getText()) {
            case Booklet booklet -> new BookletScreen(item.getDescription(),
                    List.of(BookletLayout.lay(booklet, traditional)),
                    booklet.getFigure().map(figure -> new ItemStack(figure.getItem())).orElse(ItemStack.EMPTY),
                    traditional, null);
            case Chronicle chronicle -> new BookletScreen(item.getDescription(),
                    BookletLayout.layChronicle(chronicle, traditional), ItemStack.EMPTY, traditional, null);
        };
        minecraft.setScreen(screen);
    }

    public static void openRecipe(ResourceLocation id) {
        GuRecipe recipe = KnownRecipes.find(id).orElse(null);
        if (recipe == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean traditional = TRADITIONAL.equals(minecraft.getLanguageManager().getSelected());
        BookletLayout.Spread spread = Booklet.forRecipe(id).map(booklet -> BookletLayout.lay(booklet, traditional))
                .orElse(new BookletLayout.Spread(List.of(), null));
        ItemStack result = RecipeGhosts.getResult(recipe);
        minecraft.setScreen(new BookletScreen(result.getHoverName(), List.of(spread), ItemStack.EMPTY, traditional,
                recipe));
    }

    private static List<Glyph> glyphs(List<BookletLayout.Cell> cells, ResourceLocation font) {
        return cells.stream().map(cell -> new Glyph(Component.literal(cell.glyph())
                .withStyle(style -> style.withFont(font)).getVisualOrderText(), cell.x(), cell.y())).toList();
    }

    @Override
    protected void init() {
        leftPos = (width - BookletLayout.WIDTH) / 2;
        topPos = (height - BookletLayout.HEIGHT) / 2;
        if (pages.size() < 2) return;
        int y = topPos + BookletLayout.HEIGHT - TURN_LIFT;
        next = addRenderableWidget(new PageButton(leftPos - TURN_WIDTH - 1, y, false, button -> turn(1), false));
        previous = addRenderableWidget(new PageButton(leftPos + BookletLayout.WIDTH + 1, y, true,
                button -> turn(-1), false));
        updateButtons();
    }

    //region page turning [翻页]
    private boolean turn(int step) {
        int target = Math.clamp(page + step, 0, pages.size() - 1);
        if (target == page) return false;
        page = target;
        updateButtons();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        return true;
    }

    private void updateButtons() {
        if (next != null) next.visible = page + 1 < pages.size();
        if (previous != null) previous.visible = page > 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pages.size() > 1) {
            switch (keyCode) {
                case InputConstants.KEY_LEFT, InputConstants.KEY_PAGEDOWN -> {
                    turn(1);
                    return true;
                }
                case InputConstants.KEY_RIGHT, InputConstants.KEY_PAGEUP -> {
                    turn(-1);
                    return true;
                }
                default -> { }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) return turn(scrollY < 0.0 ? 1 : -1);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //endregion

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        SpriteBatch.draw(graphics, sprites ->
                sprites.blit(SPREAD, leftPos, topPos, BookletLayout.WIDTH, BookletLayout.HEIGHT));
        Page shown = pages.get(page);
        BookletLayout.Plate plate = shown.plate();
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
            for (Glyph glyph : shown.glyphs()) {
                graphics.drawString(font, glyph.text(), leftPos + glyph.x(), topPos + glyph.y(), INK, false);
            }
        });
        for (BookletLayout.Dot dot : shown.dots()) renderDot(graphics, dot);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDot(GuiGraphics graphics, BookletLayout.Dot dot) {
        int x = leftPos + dot.x();
        int y = topPos + dot.y();
        int size = dot.size();
        if (!dot.stop()) {
            graphics.fill(x, y, x + size, y + size, VERMILION);
            return;
        }
        graphics.fill(x, y, x + size, y + 1, VERMILION);
        graphics.fill(x, y + size - 1, x + size, y + size, VERMILION);
        graphics.fill(x, y + 1, x + 1, y + size - 1, VERMILION);
        graphics.fill(x + size - 1, y + 1, x + size, y + size - 1, VERMILION);
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

    private record Page(List<Glyph> glyphs, List<BookletLayout.Dot> dots, @Nullable BookletLayout.Plate plate) {}
}
