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
 * 本包各界面共用的绘制部件。
 * <ul>
 *   <li>经 {@link SpriteBatch} 画 GUI sprite：面板、弹窗、物品格、三态文字按钮（可用、悬停、失效）、{@code <-} 返回键、
 *       进度条；另有 1 像素标题分隔线（填色）、按钮命中判定、容器按钮点击、返回键与 Esc 共用的回 B 面板入口。</li>
 *   <li>按钮分两次调用：{@link #drawButton} 进 sprite 批次，{@link #drawButtonLabel} 在后，界面才能先画全部 sprite
 *       再写字，返回键同理；标签统一水平居中、比正中低 1 像素，布局不同的按钮自画标签。</li>
 *   <li>格子即所框的 16 像素槽位，sprite 四周各大 1 像素；面板边框宽 {@link #FRAME} 像素，面板或弹窗内的内容都避开它；
 *       弹窗 sprite 右下缘外带 2 像素阴影，故比所框弹窗大这么多。</li>
 * </ul>
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

    /** {@code filled} 是填充覆盖的内宽，取 0 至 {@code width - 2}；填充内缩 1 像素。 */
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

    /** {@code escapeHeld}：由 Esc 带到这里，面板忽略该键直到松开。 */
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
