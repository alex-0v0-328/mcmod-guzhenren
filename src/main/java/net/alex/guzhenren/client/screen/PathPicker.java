package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The secondary-path [副流派] picker the B panel opens over itself: a grid of every path plus "none",
 * centred on the panel. A click on a cell sends {@link SetSecondaryPathPayload} for the aperture it was
 * opened for; a click anywhere else, Esc or B closes it without a choice.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
 */

final class PathPicker {

    private static final int COLS = 4;
    private static final int CELL_W = 84;
    private static final int CELL_H = 14;
    private static final int PAD = 8;
    private static final int FILL = 0xF0000000;
    private boolean open;
    private int aperture = ApertureData.PRIMARY;

    boolean isOpen() { return open; }

    void open(int forAperture) {
        aperture = forAperture;
        open = true;
    }

    void close() { open = false; }

    void render(GuiGraphics graphics, Font font, int panelLeft, int panelTop, int panelW, int panelH, int mouseX,
                int mouseY, int accent) {
        int x0 = left(panelLeft, panelW);
        int y0 = top(panelTop, panelH);
        int w = width();
        int h = height();

        graphics.fill(x0, y0, x0 + w, y0 + h, FILL);
        graphics.renderOutline(x0, y0, w, h, ModPalette.BORDER);
        graphics.drawString(font, Component.translatable("guzhenren.screen.pick.title"),
                x0 + PAD, y0 + (PlayerInfoScreen.HEADER_H - font.lineHeight) / 2, accent, false);
        graphics.fill(x0 + PAD, y0 + PlayerInfoScreen.HEADER_H, x0 + w - PAD, y0 + PlayerInfoScreen.HEADER_H + 1,
                PlayerInfoScreen.DIVIDER);

        for (int i = 0; i < count(); i++) {
            int cx = x0 + PAD + (i % COLS) * CELL_W;
            int cy = y0 + PAD + PlayerInfoScreen.HEADER_H + (i / COLS) * CELL_H;
            boolean hover = mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H;
            if (hover) graphics.fill(cx, cy, cx + CELL_W, cy + CELL_H, PlayerInfoScreen.ROW_HOVER);
            graphics.drawString(font, ModDisplayText.path(path(i)), cx + 3,
                    cy + (CELL_H - font.lineHeight) / 2, ModPalette.TEXT, false);
        }
    }

    void click(double mouseX, double mouseY, int panelLeft, int panelTop, int panelW, int panelH) {
        int x0 = left(panelLeft, panelW) + PAD;
        int y0 = top(panelTop, panelH) + PAD + PlayerInfoScreen.HEADER_H;
        for (int i = 0; i < count(); i++) {
            int cx = x0 + (i % COLS) * CELL_W;
            int cy = y0 + (i / COLS) * CELL_H;
            if (mouseX < cx || mouseX >= cx + CELL_W || mouseY < cy || mouseY >= cy + CELL_H) continue;

            PacketDistributor.sendToServer(new SetSecondaryPathPayload(aperture, path(i)));
            open = false;
            return;
        }
        open = false;
    }

    private static int count() { return GuPath.values().length + 1; }

    private static @Nullable GuPath path(int index) { return index == 0 ? null : GuPath.values()[index - 1]; }

    private static int rows() { return (count() + COLS - 1) / COLS; }

    private static int width() { return COLS * CELL_W + PAD * 2; }

    private static int height() { return rows() * CELL_H + PAD * 2 + PlayerInfoScreen.HEADER_H; }

    private static int left(int panelLeft, int panelW) { return panelLeft + (panelW - width()) / 2; }

    private static int top(int panelTop, int panelH) { return panelTop + (panelH - height()) / 2; }
}
