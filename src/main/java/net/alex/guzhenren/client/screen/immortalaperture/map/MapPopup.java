package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 对象栏键的弹窗共用的排版（Alex 定的四种：选取格、列表、数量窗、行菜单）：{@code popup} 底，对齐到发起它的键，贴在对象栏上方
 * {@value #GAP} 像素，左右不出界；标题写在左上角灰字。各弹窗的内容由对象栏自己排，本类只给矩形与几种行。
 */

final class MapPopup {

    static final int PAD = 5;
    static final int TITLE = 12;
    static final int ROW_H = Widgets.GHOST_H;
    static final int ROW_GAP = 2;
    private static final int GAP = 4;
    private static final int MARGIN = 6;

    /** 一枚弹窗的矩形。 */
    record Box(int x, int y, int width, int height) {

        boolean contains(double mouseX, double mouseY) { return Widgets.isOver(mouseX, mouseY, x, y, width, height); }

        int innerX() { return x + PAD; }

        int innerY() { return y + PAD + TITLE; }

        int innerWidth() { return width - 2 * PAD; }
    }

    private MapPopup() {}

    /** 宽 {@code width}、内容高 {@code content} 的弹窗，水平居中于 {@code centreX}，底边贴着 {@code above} 上方。 */
    static Box above(int centreX, int above, int width, int content, int screenWidth) {
        int height = PAD + TITLE + content + PAD;
        int x = Math.clamp(centreX - width / 2, MARGIN, Math.max(MARGIN, screenWidth - MARGIN - width));
        return new Box(x, above - GAP - height, width, height);
    }

    static void frame(SpriteBatch sprites, Box box) { sprites.blit(Widgets.POPUP, box.x, box.y, box.width, box.height); }

    static void title(GuiGraphics graphics, Font font, Box box, Component title, Component readout) {
        graphics.drawString(font, title, box.x + PAD, box.y + PAD, ModPalette.LEGEND_TEXT, false);
        Widgets.right(graphics, font, readout, box.x + box.width - PAD, box.y + PAD, ModPalette.TEXT_DEAD);
    }

    static int rowY(Box box, int row) { return box.innerY() + row * (ROW_H + ROW_GAP); }

    static int rows(int count) { return count <= 0 ? 0 : count * (ROW_H + ROW_GAP) - ROW_GAP; }

    /** 光标下是第几行（{@code width} 行宽），不在任何行上为 -1。 */
    static int rowAt(Box box, int count, int width, double mouseX, double mouseY) {
        for (int row = 0; row < count; row++) {
            if (Widgets.isOver(mouseX, mouseY, box.innerX(), rowY(box, row), width, ROW_H)) return row;
        }
        return -1;
    }
}
