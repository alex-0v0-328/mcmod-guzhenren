package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The drawing pieces every screen of this package shares, all from fills and {@link ModPalette}, no textures:
 * a bordered panel, a one-pixel header rule, item cells, a text button in its live, hovered and dead fills, the
 * {@code <-} back button that returns to the B panel, the hit test behind every button, and the container button
 * click.
 *
 * <p>A button is two calls, {@link #fillButton} and {@link #drawButtonLabel}, so a screen draws every button
 * fill before any label inside one {@link net.alex.guzhenren.client.GuiBatch}; the back button splits the same
 * way. The label sits centered one pixel below the middle, the same for every button that uses it; a button laid
 * out otherwise draws its own label. A dead button keeps its label readable in the idle fill's colour.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
 */

final class Widgets {

    private Widgets() {}

    static final int CELL = 16;
    private static final int BACK_W = 16;
    private static final int BACK_H = 14;
    private static final Component BACK = Component.literal("<-");

    static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height, ModPalette.BORDER);
    }

    static void drawRule(GuiGraphics graphics, int left, int y, int right, int color) {
        graphics.fill(left, y, right, y + 1, color);
    }

    static void drawCells(GuiGraphics graphics, int x, int y, int cols, int rows, int pitch, int fill) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) drawCell(graphics, x + col * pitch, y + row * pitch, fill);
        }
    }

    static void drawCell(GuiGraphics graphics, int x, int y, int fill) {
        graphics.fill(x, y, x + CELL, y + CELL, fill);
    }

    static void fillButton(GuiGraphics graphics, int x, int y, int width, int height, boolean live, boolean hover) {
        graphics.fill(x, y, x + width, y + height, getButtonFill(live, hover));
    }

    static void drawButtonLabel(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height,
                                boolean live) {
        drawCentered(graphics, font, label, x, y, width, height, live ? ModPalette.TEXT : ModPalette.BUTTON_IDLE);
    }

    static int getButtonFill(boolean live, boolean hover) {
        return !live ? ModPalette.BUTTON_DEAD : hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE;
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height,
                             int colour) {
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - font.lineHeight) / 2 + 1,
                colour, false);
    }

    static void fillBack(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        fillButton(graphics, x, y, BACK_W, BACK_H, true, isOverBack(mouseX, mouseY, x, y));
    }

    static void drawBackLabel(GuiGraphics graphics, Font font, int x, int y) {
        drawButtonLabel(graphics, font, BACK, x, y, BACK_W, BACK_H, true);
    }

    static boolean isOverBack(double mouseX, double mouseY, int x, int y) {
        return isOver(mouseX, mouseY, x, y, BACK_W, BACK_H);
    }

    static boolean openInfo(Screen from) {
        from.onClose();
        Minecraft.getInstance().setScreen(new PlayerInfoScreen());
        return true;
    }

    static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static boolean clickMenuButton(int containerId, int button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) return false;

        minecraft.gameMode.handleInventoryButtonClick(containerId, button);
        return true;
    }
}
