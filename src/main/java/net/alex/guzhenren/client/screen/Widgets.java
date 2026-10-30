package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The drawing pieces every screen of this package shares, drawn from the GUI sprites through a {@link SpriteBatch}:
 * the panel, the popup, item cells, a text button in its live, hovered and dead states, the {@code <-} back button
 * that returns to the B panel, and the progress bar; plus the one-pixel header rule (a fill), the hit test behind
 * every button, the container button click, and the way back to the B panel that the back button and Esc share.
 *
 * <p>A button is two calls, {@link #drawButton} into the sprite batch and {@link #drawButtonLabel} after it, so a
 * screen draws every sprite before any label; the back button splits the same way. The label sits centered one
 * pixel below the middle, the same for every button that uses it; a button laid out otherwise draws its own label.
 * A cell is the 16-pixel slot it frames, its sprite one pixel larger all round. The panel frame is {@link #FRAME}
 * pixels wide; anything drawn inside a panel or popup keeps clear of it. A popup's sprite carries a two-pixel shadow
 * past its bottom-right edge, so it draws that much larger than the popup it frames.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
 */

final class Widgets {

    private Widgets() {}

    static final int CELL = 16;
    static final int FRAME = 3;
    static final ResourceLocation SLOT_CORE = Guzhenren.id("slot_core");
    private static final ResourceLocation PANEL = Guzhenren.id("panel");
    private static final ResourceLocation POPUP = Guzhenren.id("popup");
    private static final ResourceLocation SLOT = Guzhenren.id("slot");
    private static final ResourceLocation BUTTON = Guzhenren.id("button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = Guzhenren.id("button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED = Guzhenren.id("button_disabled");
    private static final ResourceLocation BAR_BACKGROUND = Guzhenren.id("bar_background");
    private static final ResourceLocation BAR_FILL = Guzhenren.id("bar_fill");
    private static final int POPUP_SHADOW = 2;
    private static final int BACK_W = 16;
    private static final int BACK_H = 14;
    private static final Component BACK = Component.literal("<-");

    static void drawPanel(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(PANEL, x, y, width, height);
    }

    static void drawPopup(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(POPUP, x, y, width + POPUP_SHADOW, height + POPUP_SHADOW);
    }

    static void drawRule(GuiGraphics graphics, int left, int y, int right, int color) {
        graphics.fill(left, y, right, y + 1, color);
    }

    static void drawCells(SpriteBatch sprites, int x, int y, int cols, int rows, int pitch) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) drawCell(sprites, SLOT, x + col * pitch, y + row * pitch);
        }
    }

    static void drawCell(SpriteBatch sprites, int x, int y) { drawCell(sprites, SLOT, x, y); }

    static void drawCell(SpriteBatch sprites, ResourceLocation sprite, int x, int y) {
        sprites.blit(sprite, x - 1, y - 1, CELL + 2, CELL + 2);
    }

    static void drawButton(SpriteBatch sprites, int x, int y, int width, int height, boolean live, boolean hover) {
        sprites.blit(getButtonSprite(live, hover), x, y, width, height);
    }

    static ResourceLocation getButtonSprite(boolean live, boolean hover) {
        return !live ? BUTTON_DISABLED : hover ? BUTTON_HIGHLIGHTED : BUTTON;
    }

    static void drawButtonLabel(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height,
                                boolean live) {
        drawCentered(graphics, font, label, x, y, width, height, live ? ModPalette.TEXT : ModPalette.TEXT_DEAD);
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height,
                             int colour) {
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - font.lineHeight) / 2 + 1,
                colour, false);
    }

    /** {@code filled} is the inner width the fill covers, from 0 to {@code width - 2}; the fill sits 1 pixel in. */
    static void drawBar(SpriteBatch sprites, int x, int y, int width, int height, int filled, int colour) {
        sprites.blit(BAR_BACKGROUND, x, y, width, height);
        sprites.blit(BAR_FILL, x + 1, y + 1, Math.min(filled, width - 2), height - 2, colour);
    }

    static void drawBack(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        drawButton(sprites, x, y, BACK_W, BACK_H, true, isOverBack(mouseX, mouseY, x, y));
    }

    static void drawBackLabel(GuiGraphics graphics, Font font, int x, int y) {
        drawButtonLabel(graphics, font, BACK, x, y, BACK_W, BACK_H, true);
    }

    static boolean isOverBack(double mouseX, double mouseY, int x, int y) {
        return isOver(mouseX, mouseY, x, y, BACK_W, BACK_H);
    }

    static boolean openInfo(Screen from) { return openInfo(from, false); }

    /** {@code escapeHeld}: Esc brought it here, so the panel ignores that key until it is released. */
    static boolean openInfo(Screen from, boolean escapeHeld) {
        from.onClose();
        Minecraft.getInstance().setScreen(new PlayerInfoScreen(escapeHeld));
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
