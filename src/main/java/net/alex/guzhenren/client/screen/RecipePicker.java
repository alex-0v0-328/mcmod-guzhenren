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
 * 叠加在炼蛊界面之上的蛊方选择弹窗。
 * <ul>
 *   <li>首行固定为「自动」，其后列出所有已知蛊方及其产物图标与基础成功率；与 B 面板弹窗一样按整行开窗（每次至多显示
 *       {@link #MAX_ROWS} 行并支持滚轮滚动），鼠标悬停时列出所需材料与消耗的提示框则在批处理结束之后再画（原版提示自
 *       带独立批次，不可嵌套）。</li>
 *   <li>{@link #click} 返回选中行对应的菜单按钮 ID 并由界面发送：「自动」发送清除按钮，具体蛊方发送
 *       {@code BUTTON_RECIPE_BASE} 加上配方序号。</li>
 *   <li>⚠ 绘制时必须沿 Z 轴外移 {@link #Z} 深度，才能正确盖过下方的物品槽位层。</li>
 *   <li>整块弹窗走单个 {@link GuiBatch} 并分四遍提交（弹窗精灵图、填色、自行提交的产物图标、文字），各行之间互不重
 *       叠且每行内部层序不变，保证悬停与选中填色（选中左侧 2 像素金边）不会压盖弹窗边框；弹窗下层先压暗
 *       （{@link Widgets#drawScrim}）。</li>
 * </ul>
 */

final class RecipePicker {

    static final int NO_BUTTON = -1;
    private static final int W = 200;
    private static final int PAD = 6;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 20;
    private static final int MAX_ROWS = 5;
    private static final float Z = 500.0F;
    private static final String STONE_SEPARATOR = " · ";
    private static final Component TITLE = Component.translatable("guzhenren.menu.refinement.pick.title");
    private static final Component AUTO = Component.translatable("guzhenren.menu.refinement.pick.auto");
    private static final Component EMPTY = Component.translatable("guzhenren.menu.refinement.pick.empty");
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

        Widgets.drawScrim(graphics);
        GuiBatch.draw(graphics, () -> {
            fillFrame(graphics, selected, hovered, x0, y0, visible);
            for (int i = 0; i < visible; i++) drawIcon(graphics, known, scroll + i, x0, getRowY(y0, i));
            graphics.drawString(font, TITLE, x0 + PAD,
                    y0 + (HEADER_H - font.lineHeight) / 2, ModPalette.ACCENT, false);
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
        graphics.fill(x0 + 1, y0 + 1, x0 + W - 1, y0 + HEADER_H, ModPalette.SURFACE_2);
        int left = x0 + Widgets.FRAME;
        int width = W - Widgets.FRAME * 2;
        for (int i = 0; i < visible; i++) {
            int row = scroll + i;
            int rowY = getRowY(y0, i);
            if (row - 1 == selected) {
                graphics.fill(left, rowY, left + width, rowY + ROW_H, ModPalette.SURFACE_3);
                graphics.fill(left, rowY, left + 2, rowY + ROW_H, ModPalette.ACCENT);
            } else if (row == hovered) {
                graphics.fill(left, rowY, left + width, rowY + ROW_H, ModPalette.SURFACE_2);
            }
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
            graphics.drawString(font, known.isEmpty() ? EMPTY : AUTO, x0 + PAD, textY, ModPalette.TEXT, false);
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
