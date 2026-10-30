package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The Gu Recipe [蛊方] picker the refinement screen opens over itself: "automatic" first, then every known
 * recipe with its result and base chance, a hover tooltip listing what it needs and costs. Rows are windowed
 * whole, like the B panel's modal, {@link #MAX_ROWS} at a time; the scroll wheel moves the window.
 *
 * <p>{@link #click} returns the menu button the chosen row stands for -- clear for "automatic", or the
 * recipe's index past {@code BUTTON_RECIPE_BASE} -- and the screen sends it. ⚠ It is drawn {@link #Z} out so
 * it clears the item layers below it.
 *
 * <p>It draws in one {@link GuiBatch}, in four passes that keep each row's own order: the popup sprite, every fill,
 * then every result icon (an item flushes on its own), then every text. Rows never overlap, so only the order within
 * a row shows; a row's hover fill and selection outline keep clear of the popup frame. The hover tooltip draws after
 * the batch: a tooltip runs a batch of its own, and batches never nest.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

final class RecipePicker {

    static final int NO_BUTTON = -1;
    private static final int W = 200;
    private static final int PAD = 6;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 20;
    private static final int MAX_ROWS = 5;
    private static final float Z = 500.0F;
    private static final int RULE = 0x66FFFFFF;
    private static final int ROW_HOVER = 0x33FFFFFF;
    private static final String STONE_SEPARATOR = " · ";
    private static final String TITLE_KEY = "guzhenren.menu.refinement.pick.title";
    private static final String AUTO_KEY = "guzhenren.menu.refinement.pick.auto";
    private static final String EMPTY_KEY = "guzhenren.menu.refinement.pick.empty";
    private static final String NEEDS_KEY = "guzhenren.menu.refinement.pick.needs";
    private static final String ITEM_KEY = "guzhenren.menu.refinement.pick.item";
    private static final String WINDOWS_KEY = "guzhenren.menu.refinement.pick.windows";
    private static final String STONES_KEY = "guzhenren.menu.refinement.pick.stones";
    private static final String COST_KEY = "guzhenren.menu.refinement.pick.cost";
    private static final String SOUL_KEY = "guzhenren.menu.refinement.pick.soul";
    private static final String CHANCE_KEY = "guzhenren.menu.refinement.pick.chance";
    private static final String SUCCESS_KEY = "guzhenren.menu.refinement.pick.success";
    private boolean open;
    private int scroll;

    boolean isOpen() { return open; }

    void open() {
        open = true;
        scroll = 0;
    }

    void close() { open = false; }

    void scroll(double delta) {
        if (delta != 0.0) scroll = Math.max(0, scroll - (int) Math.signum(delta));
    }

    void render(GuiGraphics graphics, Font font, List<GuRecipe> known, int selected, int panelLeft,
                int panelTop, int panelW, int panelH, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z);
        draw(graphics, font, known, selected, panelLeft, panelTop, panelW, panelH, mouseX, mouseY);
        graphics.pose().popPose();
    }

    int click(double mouseX, double mouseY, List<GuRecipe> known, int panelLeft, int panelTop,
              int panelW, int panelH) {
        int visible = Math.min(MAX_ROWS, known.size() + 1);
        int x0 = getLeft(panelLeft, panelW);
        int y0 = getTop(panelTop, panelH, visible);
        open = false;

        for (int i = 0; i < visible; i++) {
            int rowY = getRowY(y0, i);
            if (!Widgets.isOver(mouseX, mouseY, x0, rowY, W, ROW_H)) continue;

            int row = scroll + i;
            return row == 0 ? RefinementMenu.BUTTON_CLEAR_RECIPE : RefinementMenu.BUTTON_RECIPE_BASE + row - 1;
        }
        return NO_BUTTON;
    }

    private void draw(GuiGraphics graphics, Font font, List<GuRecipe> known, int selected,
                      int panelLeft, int panelTop, int panelW, int panelH, int mouseX, int mouseY) {
        int rows = known.size() + 1;
        int visible = Math.min(MAX_ROWS, rows);
        scroll = Mth.clamp(scroll, 0, rows - visible);

        int x0 = getLeft(panelLeft, panelW);
        int y0 = getTop(panelTop, panelH, visible);
        int hovered = getHoveredRow(x0, y0, visible, mouseX, mouseY);

        GuiBatch.draw(graphics, () -> {
            fillFrame(graphics, selected, hovered, x0, y0, visible);
            for (int i = 0; i < visible; i++) drawIcon(graphics, known, scroll + i, x0, getRowY(y0, i));
            graphics.drawString(font, Component.translatable(TITLE_KEY), x0 + PAD,
                    y0 + (HEADER_H - font.lineHeight) / 2, ModPalette.REFINEMENT, false);
            for (int i = 0; i < visible; i++) drawText(graphics, font, known, scroll + i, x0, getRowY(y0, i));
        });
        if (hovered >= 1) {
            graphics.renderComponentTooltip(font, formatDetails(known.get(hovered - 1)), mouseX, mouseY);
        }
    }

    private int getHoveredRow(int x0, int y0, int visible, int mouseX, int mouseY) {
        for (int i = 0; i < visible; i++) {
            if (Widgets.isOver(mouseX, mouseY, x0, getRowY(y0, i), W, ROW_H)) return scroll + i;
        }
        return -1;
    }

    private void fillFrame(GuiGraphics graphics, int selected, int hovered, int x0, int y0, int visible) {
        SpriteBatch.draw(graphics, sprites -> Widgets.drawPopup(sprites, x0, y0, W, getHeight(visible)));
        Widgets.drawRule(graphics, x0 + PAD, y0 + HEADER_H, x0 + W - PAD, RULE);
        int left = x0 + Widgets.FRAME;
        int width = W - Widgets.FRAME * 2;
        for (int i = 0; i < visible; i++) {
            int row = scroll + i;
            int rowY = getRowY(y0, i);
            if (row == hovered) graphics.fill(left, rowY, left + width, rowY + ROW_H, ROW_HOVER);
            if (row - 1 == selected) graphics.renderOutline(left, rowY, width, ROW_H, ModPalette.REFINEMENT);
        }
    }

    private static void drawIcon(GuiGraphics graphics, List<GuRecipe> known, int row, int x0, int y) {
        if (row == 0) return;
        ItemStack icon = RecipeGhosts.getResult(known.get(row - 1));
        graphics.renderFakeItem(icon, x0 + PAD, y + (ROW_H - Widgets.CELL) / 2);
    }

    private static void drawText(GuiGraphics graphics, Font font, List<GuRecipe> known, int row, int x0,
                                 int y) {
        int textY = y + (ROW_H - font.lineHeight) / 2;
        if (row == 0) {
            Component auto = Component.translatable(known.isEmpty() ? EMPTY_KEY : AUTO_KEY);
            graphics.drawString(font, auto, x0 + PAD, textY, ModPalette.TEXT, false);
            return;
        }
        GuRecipe recipe = known.get(row - 1);
        graphics.drawString(font, RecipeGhosts.getResult(recipe).getHoverName(), x0 + PAD + Widgets.CELL + 4, textY,
                ModPalette.TEXT, false);

        Component rate = Component.translatable(CHANCE_KEY, recipe.baseSuccess());
        graphics.drawString(font, rate, x0 + W - PAD - font.width(rate), textY, ModPalette.LEGEND_TEXT, false);
    }

    private static List<Component> formatDetails(GuRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(RecipeGhosts.getResult(recipe).getHoverName());
        lines.add(Component.translatable(NEEDS_KEY));
        for (GuIngredient need : recipe.ingredients()) {
            lines.add(Component.translatable(ITEM_KEY, need.count(), RecipeGhosts.getOption(need).getHoverName()));
        }
        lines.add(Component.translatable(WINDOWS_KEY, recipe.getWindowCount(), recipe.getTotalSeconds()));
        lines.add(Component.translatable(STONES_KEY, formatStoneList(recipe)));
        lines.add(Component.translatable(COST_KEY, recipe.essencePerSecond()));
        lines.add(Component.translatable(SOUL_KEY, recipe.soulPerSecond()));
        lines.add(Component.translatable(SUCCESS_KEY, recipe.baseSuccess()));
        return lines;
    }

    private static String formatStoneList(GuRecipe recipe) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < recipe.getWindowCount(); i++) {
            if (i > 0) text.append(STONE_SEPARATOR);
            text.append(recipe.getStonesFor(i));
        }
        return text.toString();
    }

    private static int getHeight(int visible) { return HEADER_H + PAD * 2 + visible * ROW_H; }

    private static int getLeft(int panelLeft, int panelW) { return panelLeft + (panelW - W) / 2; }

    private static int getTop(int panelTop, int panelH, int visible) {
        return panelTop + (panelH - getHeight(visible)) / 2;
    }

    private static int getRowY(int y0, int index) { return y0 + HEADER_H + PAD + index * ROW_H; }
}
