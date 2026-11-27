package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapCamera;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapContours;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapOverlay;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapSurface;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMark;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页右下角的图例框（{@code overlay} 半透明底）：悬停读数（x / y / z，迷雾处写「未知」）、当前贴图的高度范围、俯视时的
 * 等高距说明、四种活物标记的颜色。
 * <ul>
 *   <li>高度标尺已移到右缘竖排（{@link MapHeightRail}，Alex 定），本框高 {@value #HEIGHT}。</li>
 *   <li>标记颜色 {@link #colour} 也供地图上的标记点用；玩家画头像，不进图例。</li>
 * </ul>
 */

final class MapLegend {

    static final int WIDTH = 136;
    static final int HEIGHT = 70;
    private static final int MARGIN = 6;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String[] MARK_KEYS = { "", "penned", "loose", "wild_gu", "spirit" };
    private static final int[] MARK_COLOURS = { 0xFFFFFFFF, 0xFF66E07A, 0xFFFFB347, 0xFFC58BFF, 0xFFF8ACC4 };
    private int left;
    private int top;

    static int colour(byte kind) { return kind >= 0 && kind < MARK_COLOURS.length ? MARK_COLOURS[kind] : 0xFFFFFFFF; }

    void layout(int width, int height) {
        left = width - MARGIN - WIDTH;
        top = height - MARGIN - HEIGHT;
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + WIDTH && mouseY >= top && mouseY < top + HEIGHT;
    }

    int top() { return top; }

    void sprites(SpriteBatch sprites) { sprites.blit(PanelDrawing.OVERLAY, left, top, WIDTH, HEIGHT); }

    void fills(GuiGraphics graphics) {
        for (int kind = MapMark.PENNED; kind <= MapMark.SPIRIT; kind++) {
            int x = left + 6 + (kind - 1) % 2 * 66;
            int y = top + 44 + (kind - 1) / 2 * 12;
            graphics.fill(x - 1, y - 1, x + 6, y + 6, MapOverlay.SHADOW);
            graphics.fill(x, y, x + 5, y + 5, MARK_COLOURS[kind]);
        }
    }

    void texts(GuiGraphics graphics, Font font, MapCamera camera, int lowest, int highest,
               @Nullable MapPick.Hit hover) {
        Component readout = hover == null ? Component.empty() : hover.known()
                ? Component.translatable(UI + "readout", hover.x(), hover.ground(), hover.z())
                : Component.translatable(UI + "unknown");
        graphics.drawString(font, readout, left + 6, top + 6, PanelDrawing.TEXT, false);
        if (lowest != MapSurface.NO_HEIGHT) {
            graphics.drawString(font, Component.translatable(UI + "height", lowest, highest), left + 6, top + 18,
                    PanelDrawing.LEGEND, false);
        }
        if (camera.plan()) {
            graphics.drawString(font, Component.translatable(UI + "contour", MapContours.MINOR_STEP,
                    MapContours.MAJOR_STEP), left + 6, top + 30, PanelDrawing.LEGEND, false);
        }
        for (int kind = MapMark.PENNED; kind <= MapMark.SPIRIT; kind++) {
            int x = left + 6 + (kind - 1) % 2 * 66;
            int y = top + 44 + (kind - 1) / 2 * 12;
            graphics.drawString(font, Component.translatable(UI + "mark." + MARK_KEYS[kind]), x + 8, y - 1,
                    PanelDrawing.LEGEND, false);
        }
    }
}
