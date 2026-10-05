package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.refinement.GuRecipe;
import net.alex.guzhenren.gameplay.refinement.RefinementMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * The Gu Recipe [蛊方] picker the refinement screen opens over itself: "automatic" first, then every known
 * recipe with its result and base chance, a hover tooltip listing what it needs and costs. Rows are windowed
 * whole, like the B panel's modal, {@link #MAX_ROWS} at a time; the scroll wheel moves the window.
 *
 * <p>{@link #click} returns the menu button the chosen row stands for -- clear for "automatic", or the
 * recipe's index past {@code BUTTON_RECIPE_BASE} -- and the screen sends it. ⚠ It is drawn {@link #Z} out so
 * it clears the item layers below it.
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
    private static final int HEADER_H = 14;
    private static final int ROW_H = 20;
    private static final int MAX_ROWS = 5;
    private static final float Z = 500.0F;
    private static final int FILL = 0xF0000000;
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

    void render(GuiGraphics graphics, Font font, List<RecipeHolder<GuRecipe>> known, int selected, int panelLeft,
                int panelTop, int panelW, int panelH, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z);
        draw(graphics, font, known, selected, panelLeft, panelTop, panelW, panelH, mouseX, mouseY);
        graphics.pose().popPose();
    }

    int click(double mouseX, double mouseY, List<RecipeHolder<GuRecipe>> known, int panelLeft, int panelTop,
              int panelW, int panelH) {
        int visible = Math.min(MAX_ROWS, known.size() + 1);
        int x0 = left(panelLeft, panelW);
        int y0 = top(panelTop, panelH, visible);
        open = false;

        for (int i = 0; i < visible; i++) {
            int rowY = rowY(y0, i);
            if (mouseX < x0 || mouseX >= x0 + W || mouseY < rowY || mouseY >= rowY + ROW_H) continue;

            int row = scroll + i;
            return row == 0 ? RefinementMenu.BUTTON_CLEAR_RECIPE : RefinementMenu.BUTTON_RECIPE_BASE + row - 1;
        }
        return NO_BUTTON;
    }

    private void draw(GuiGraphics graphics, Font font, List<RecipeHolder<GuRecipe>> known, int selected,
                      int panelLeft, int panelTop, int panelW, int panelH, int mouseX, int mouseY) {
        int rows = known.size() + 1;
        int visible = Math.min(MAX_ROWS, rows);
        scroll = Mth.clamp(scroll, 0, rows - visible);

        int x0 = left(panelLeft, panelW);
        int y0 = top(panelTop, panelH, visible);
        int h = height(visible);

        graphics.fill(x0, y0, x0 + W, y0 + h, FILL);
        graphics.renderOutline(x0, y0, W, h, ModPalette.BORDER);
        graphics.drawString(font, Component.translatable(TITLE_KEY), x0 + PAD,
                y0 + (HEADER_H - font.lineHeight) / 2, ModPalette.REFINEMENT, false);
        graphics.fill(x0 + PAD, y0 + HEADER_H, x0 + W - PAD, y0 + HEADER_H + 1, ModPalette.BORDER);

        int hovered = -1;
        for (int i = 0; i < visible; i++) {
            int row = scroll + i;
            int rowY = rowY(y0, i);
            if (mouseX >= x0 && mouseX < x0 + W && mouseY >= rowY && mouseY < rowY + ROW_H) {
                graphics.fill(x0 + 1, rowY, x0 + W - 1, rowY + ROW_H, ModPalette.BUTTON_IDLE);
                hovered = row;
            }
            if (row - 1 == selected) graphics.renderOutline(x0 + 1, rowY, W - 2, ROW_H, ModPalette.REFINEMENT);
            drawRow(graphics, font, known, row, x0, rowY);
        }
        if (hovered >= 1) {
            graphics.renderComponentTooltip(font, details(known.get(hovered - 1).value()), mouseX, mouseY);
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, List<RecipeHolder<GuRecipe>> known, int row, int x0,
                                int y) {
        int textY = y + (ROW_H - font.lineHeight) / 2;
        if (row == 0) {
            Component auto = Component.translatable(known.isEmpty() ? EMPTY_KEY : AUTO_KEY);
            graphics.drawString(font, auto, x0 + PAD, textY, ModPalette.TEXT, false);
            return;
        }
        GuRecipe recipe = known.get(row - 1).value();
        ItemStack icon = RefinementScreen.result(recipe);
        graphics.renderFakeItem(icon, x0 + PAD, y + (ROW_H - RefinementScreen.CELL) / 2);
        graphics.drawString(font, icon.getHoverName(), x0 + PAD + RefinementScreen.CELL + 4, textY, ModPalette.TEXT,
                false);

        Component rate = Component.translatable(CHANCE_KEY, recipe.baseSuccess());
        graphics.drawString(font, rate, x0 + W - PAD - font.width(rate), textY, RefinementScreen.LEGEND_TEXT, false);
    }

    private static List<Component> details(GuRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(RefinementScreen.result(recipe).getHoverName());
        lines.add(Component.translatable(NEEDS_KEY));
        for (SizedIngredient need : recipe.ingredients()) {
            lines.add(Component.translatable(ITEM_KEY, need.count(), RefinementScreen.option(need).getHoverName()));
        }
        lines.add(Component.translatable(WINDOWS_KEY, recipe.windowCount(), recipe.totalSeconds()));
        lines.add(Component.translatable(STONES_KEY, stoneList(recipe)));
        lines.add(Component.translatable(COST_KEY, recipe.essencePerSecond()));
        lines.add(Component.translatable(SOUL_KEY, recipe.soulPerSecond()));
        lines.add(Component.translatable(SUCCESS_KEY, recipe.baseSuccess()));
        return lines;
    }

    private static String stoneList(GuRecipe recipe) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < recipe.windowCount(); i++) {
            if (i > 0) text.append(STONE_SEPARATOR);
            text.append(recipe.stonesFor(i));
        }
        return text.toString();
    }

    private static int height(int visible) { return HEADER_H + PAD * 2 + visible * ROW_H; }

    private static int left(int panelLeft, int panelW) { return panelLeft + (panelW - W) / 2; }

    private static int top(int panelTop, int panelH, int visible) { return panelTop + (panelH - height(visible)) / 2; }

    private static int rowY(int y0, int index) { return y0 + HEADER_H + PAD + index * ROW_H; }
}
