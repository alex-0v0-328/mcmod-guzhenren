package net.alex.guzhenren.client.screen.immortalaperture;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 养殖与蛊虫小节共用的「保持数」列表卡的排版：标题带、表头（名字 / 数量 / 托管 / 保持）、凹槽里每种一行（图标、名字、数量、
 * 托管开关、幽灵键 −/+ 夹着保持数），卡底一行状态、一行右对齐按钮。
 * <ul>
 *   <li>卡宽至多 {@value #MAX_W}，靠内容区左边；行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），种类多于
 *       行数时行让出右侧 8 像素给滚动条。</li>
 *   <li>只管几何与通用部件；数据、文字、点击由两页各自做。</li>
 * </ul>
 */

final class KeepList {

    static final int ROW_H = 20;
    static final int BUTTON_W = 72;
    static final int BUTTON_H = 18;
    private static final int MAX_W = 420;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 10;
    private static final int INSET_Y = 30;
    private static final int FOOT_H = 12 + BUTTON_H + 2 * PanelDrawing.PAD;
    private int x;
    private int y;
    private int w = MAX_W;
    private int rows = MIN_ROWS;

    void layout(int width, int height) {
        x = PanelDrawing.contentX(width);
        y = InfoPage.CONTENT_TOP;
        w = Math.min(PanelDrawing.contentWidth(width), MAX_W);
        rows = Math.clamp((height - y - INSET_Y - 2 - FOOT_H - PanelDrawing.MARGIN) / ROW_H, MIN_ROWS, MAX_ROWS);
    }

    int rows() { return rows; }

    int x() { return x; }

    int y() { return y; }

    int w() { return w; }

    int height() { return INSET_Y + insetH() + FOOT_H; }

    int insetX() { return x + PanelDrawing.PAD; }

    int insetY() { return y + INSET_Y; }

    int insetW() { return w - 2 * PanelDrawing.PAD; }

    int insetH() { return rows * ROW_H + 2; }

    int rowX() { return insetX() + 1; }

    int rowW(int total) { return insetW() - 2 - (total > rows ? 8 : 0); }

    int rowRight(int total) { return rowX() + rowW(total); }

    int rowTop(int index) { return insetY() + 1 + index * ROW_H; }

    int plusX(int total) { return rowRight(total) - 2 - PanelDrawing.GHOST_H; }

    int minusX(int total) { return plusX(total) - 26 - PanelDrawing.GHOST_H; }

    int switchX(int total) { return minusX(total) - 8 - PanelDrawing.SWITCH_W; }

    int countRight(int total) { return switchX(total) - 10; }

    int statusY() { return insetY() + insetH() + 6; }

    int buttonY() { return statusY() + 12; }

    int buttonX(int fromRight) { return x + w - PanelDrawing.PAD - (fromRight + 1) * BUTTON_W - fromRight * 6; }

    boolean isOverList(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, insetX(), insetY(), insetW(), insetH());
    }

    boolean isOverButton(int fromRight, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, buttonX(fromRight), buttonY(), BUTTON_W, BUTTON_H);
    }

    //region Drawing [绘制]
    void frame(SpriteBatch sprites, int total, int offset, double mouseX, double mouseY) {
        PanelDrawing.card(sprites, x, y, w, height());
        sprites.blit(PanelDrawing.INSET, insetX(), insetY(), insetW(), insetH());
        if (total > rows) {
            int barX = rowRight(total) + 1;
            PanelDrawing.scrollbar(sprites, barX, insetY() + 1, insetH() - 2, offset, rows, total,
                    Widgets.isOver(mouseX, mouseY, barX, insetY() + 1, 6, insetH() - 2));
        }
    }

    void row(SpriteBatch sprites, int index, int total, boolean hosted, double mouseX, double mouseY) {
        int top = rowTop(index);
        PanelDrawing.row(sprites, rowX(), top, rowW(total), ROW_H,
                Widgets.isOver(mouseX, mouseY, rowX(), top, rowW(total), ROW_H));
        PanelDrawing.toggle(sprites, switchX(total), top + 5, hosted, true, mouseX, mouseY);
        PanelDrawing.button(sprites, minusX(total), top + 3, PanelDrawing.GHOST_H, PanelDrawing.GHOST_H,
                PanelDrawing.Look.GHOST, hosted, mouseX, mouseY);
        PanelDrawing.button(sprites, plusX(total), top + 3, PanelDrawing.GHOST_H, PanelDrawing.GHOST_H,
                PanelDrawing.Look.GHOST, hosted, mouseX, mouseY);
    }

    void header(GuiGraphics graphics, Font font, Component first, int total) {
        int top = y + 19;
        graphics.drawString(font, first, rowX() + 4, top, PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font, Component.translatable(EarthSpiritScreen.UI + "count"), countRight(total),
                top, PanelDrawing.LEGEND);
        graphics.drawString(font, Component.translatable(EarthSpiritScreen.UI + "hosting"), switchX(total), top,
                PanelDrawing.LEGEND, false);
        PanelDrawing.centered(graphics, font, Component.translatable(EarthSpiritScreen.UI + "keep"), minusX(total),
                plusX(total) + PanelDrawing.GHOST_H, top, PanelDrawing.LEGEND);
    }

    void rowTexts(GuiGraphics graphics, Font font, int index, int total, Component name, int nameColour, int count,
                  int keep, boolean hosted) {
        int top = rowTop(index);
        graphics.drawString(font, name, rowX() + 22, top + 6, nameColour, false);
        PanelDrawing.right(graphics, font, Integer.toString(count), countRight(total), top + 6, PanelDrawing.TEXT);
        PanelDrawing.label(graphics, font, Component.literal("-"), minusX(total), top + 3, PanelDrawing.GHOST_H,
                PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST, hosted);
        PanelDrawing.label(graphics, font, Component.literal("+"), plusX(total), top + 3, PanelDrawing.GHOST_H,
                PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST, hosted);
        PanelDrawing.centered(graphics, font, Component.literal(Integer.toString(keep)),
                minusX(total) + PanelDrawing.GHOST_H, plusX(total), top + 6,
                hosted ? PanelDrawing.TEXT : PanelDrawing.DEAD);
    }
    //endregion
}
