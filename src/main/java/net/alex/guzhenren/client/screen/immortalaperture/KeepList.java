package net.alex.guzhenren.client.screen.immortalaperture;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 养殖与蛊虫小节共用的「下限 / 保持数」列表卡的排版：标题带、表头（名字 / 数量 / 下限或保持）、凹槽里每行（图标或空、名字、
 * 数量、幽灵键 −/+ 夹着数字），卡底一行状态、一行右对齐按钮。托管没有开关（Alex 定）。
 * <ul>
 *   <li>卡宽至多 {@value InfoPage#COLUMN_W}，在信息页的单列里居中（{@link InfoPage#columnX}）；行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），种类多于
 *       行数时行让出右侧 8 像素给滚动条。</li>
 *   <li>只管几何与通用部件；数据、文字、点击由两页各自做。</li>
 * </ul>
 */

final class KeepList {

    static final int ROW_H = 20;
    static final int BUTTON_W = 72;
    static final int BUTTON_H = 18;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 10;
    private static final int INSET_Y = 30;
    private static final int FOOT_H = 12 + BUTTON_H + 2 * Widgets.PAD;
    private int x;
    private int y;
    private int w = InfoPage.COLUMN_W;
    private int rows = MIN_ROWS;

    void layout(int width, int height) {
        x = InfoPage.columnX(width);
        y = InfoPage.CONTENT_TOP;
        w = InfoPage.columnWidth(width);
        rows = Math.clamp((height - y - INSET_Y - 2 - FOOT_H - PanelDrawing.MARGIN) / ROW_H, MIN_ROWS, MAX_ROWS);
    }

    int rows() { return rows; }

    int x() { return x; }

    int y() { return y; }

    int w() { return w; }

    int height() { return INSET_Y + insetH() + FOOT_H; }

    int insetX() { return x + Widgets.PAD; }

    int insetY() { return y + INSET_Y; }

    int insetW() { return w - 2 * Widgets.PAD; }

    int insetH() { return rows * ROW_H + 2; }

    int rowX() { return insetX() + 1; }

    int rowW(int total) { return insetW() - 2 - (total > rows ? 8 : 0); }

    int rowRight(int total) { return rowX() + rowW(total); }

    int rowTop(int index) { return insetY() + 1 + index * ROW_H; }

    int plusX(int total) { return rowRight(total) - 2 - Widgets.GHOST_H; }

    int minusX(int total) { return plusX(total) - 26 - Widgets.GHOST_H; }

    int countRight(int total) { return minusX(total) - 14; }

    int statusY() { return insetY() + insetH() + 6; }

    int buttonY() { return statusY() + 12; }

    int buttonX(int fromRight) { return x + w - Widgets.PAD - (fromRight + 1) * BUTTON_W - fromRight * 6; }

    boolean isOverList(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, insetX(), insetY(), insetW(), insetH());
    }

    boolean isOverButton(int fromRight, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, buttonX(fromRight), buttonY(), BUTTON_W, BUTTON_H);
    }

    //region Drawing [绘制]
    void frame(SpriteBatch sprites, int total, int offset, double mouseX, double mouseY) {
        Widgets.card(sprites, x, y, w, height());
        sprites.blit(Widgets.INSET, insetX(), insetY(), insetW(), insetH());
        if (total > rows) {
            int barX = rowRight(total) + 1;
            Widgets.scrollbar(sprites, barX, insetY() + 1, insetH() - 2, offset, rows, total,
                    Widgets.isOver(mouseX, mouseY, barX, insetY() + 1, 6, insetH() - 2));
        }
    }

    void row(SpriteBatch sprites, int index, int total, boolean live, double mouseX, double mouseY) {
        int top = rowTop(index);
        Widgets.row(sprites, rowX(), top, rowW(total), ROW_H,
                Widgets.isOver(mouseX, mouseY, rowX(), top, rowW(total), ROW_H));
        Widgets.button(sprites, minusX(total), top + 3, Widgets.GHOST_H, Widgets.GHOST_H,
                Widgets.Look.GHOST, live, mouseX, mouseY);
        Widgets.button(sprites, plusX(total), top + 3, Widgets.GHOST_H, Widgets.GHOST_H,
                Widgets.Look.GHOST, live, mouseX, mouseY);
    }

    void header(GuiGraphics graphics, Font font, Component first, Component keep, int total) {
        int top = y + 19;
        graphics.drawString(font, first, rowX() + 4, top, ModPalette.LEGEND_TEXT, false);
        Widgets.right(graphics, font, Component.translatable(EarthSpiritScreen.UI + "count"), countRight(total),
                top, ModPalette.LEGEND_TEXT);
        Widgets.centered(graphics, font, keep, minusX(total),
                plusX(total) + Widgets.GHOST_H, top, ModPalette.LEGEND_TEXT);
    }

    void rowTexts(GuiGraphics graphics, Font font, int index, int total, Component name, int nameColour, int count,
                  int keep, boolean live) {
        int top = rowTop(index);
        graphics.drawString(font, name, rowX() + 22, top + 6, nameColour, false);
        Widgets.right(graphics, font, Integer.toString(count), countRight(total), top + 6, ModPalette.TEXT);
        Widgets.label(graphics, font, Component.literal("-"), minusX(total), top + 3, Widgets.GHOST_H,
                Widgets.GHOST_H, Widgets.Look.GHOST, live);
        Widgets.label(graphics, font, Component.literal("+"), plusX(total), top + 3, Widgets.GHOST_H,
                Widgets.GHOST_H, Widgets.Look.GHOST, live);
        Widgets.centered(graphics, font, Component.literal(Integer.toString(keep)),
                minusX(total) + Widgets.GHOST_H, plusX(total), top + 6,
                live ? ModPalette.TEXT : ModPalette.TEXT_DEAD);
    }
    //endregion
}
