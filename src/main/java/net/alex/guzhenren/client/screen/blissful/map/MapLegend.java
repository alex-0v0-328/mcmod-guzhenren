package net.alex.guzhenren.client.screen.blissful.map;

import net.alex.guzhenren.client.screen.blissful.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.blissful.PanelKit;
import net.alex.guzhenren.gameplay.blissful.map.MapMark;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页右下角的图例框：悬停读数（x / y / z，迷雾处写「未知」）、当前贴图的高度范围与高度标尺、俯视时的等高距说明、
 * 四种活物标记的颜色。
 * <ul>
 *   <li>高度标尺横放，左端最低、右端最高；每 {@value MapContours#MINOR_STEP} 格一根细刻度、每
 *       {@value MapContours#MAJOR_STEP} 格一根长刻度（与等高线同距），悬停列的地面高画成白色竖线。</li>
 *   <li>标记颜色 {@link #colour} 也供地图上的标记点用；玩家画头像，不进图例。</li>
 * </ul>
 */

final class MapLegend {

    static final int WIDTH = 136;
    static final int HEIGHT = 84;
    private static final int MARGIN = 6;
    private static final int BACK = 0xD0201A1E;
    private static final int GAUGE_BACK = MapPalette.FOG;
    private static final int TICK = 0xFF888785;
    private static final int MAJOR_TICK = 0xFFC3C8CF;
    private static final int GAUGE_Y = 72;
    private static final int GAUGE_H = 6;
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

    void fills(GuiGraphics graphics, int lowest, int highest, @Nullable MapPick.Hit hover) {
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, BACK);
        graphics.fill(left, top, left + WIDTH, top + 1, PanelKit.ACCENT);
        for (int kind = MapMark.PENNED; kind <= MapMark.SPIRIT; kind++) {
            int x = left + 6 + (kind - 1) % 2 * 66;
            int y = top + 44 + (kind - 1) / 2 * 12;
            graphics.fill(x - 1, y - 1, x + 6, y + 6, MapOverlay.SHADOW);
            graphics.fill(x, y, x + 5, y + 5, MARK_COLOURS[kind]);
        }
        if (lowest == MapSurface.NO_HEIGHT || highest <= lowest) return;

        int gaugeLeft = left + 6;
        int gaugeWidth = WIDTH - 12;
        int gaugeTop = top + GAUGE_Y;
        graphics.fill(gaugeLeft, gaugeTop, gaugeLeft + gaugeWidth, gaugeTop + GAUGE_H, GAUGE_BACK);
        int first = Math.floorDiv(lowest, MapContours.MINOR_STEP) * MapContours.MINOR_STEP + MapContours.MINOR_STEP;
        for (int level = first; level < highest; level += MapContours.MINOR_STEP) {
            int x = gaugeLeft + (level - lowest) * (gaugeWidth - 1) / (highest - lowest);
            boolean major = level % MapContours.MAJOR_STEP == 0;
            graphics.fill(x, gaugeTop + (major ? 0 : 3), x + 1, gaugeTop + GAUGE_H, major ? MAJOR_TICK : TICK);
        }
        if (hover != null && hover.known()) {
            int clamped = Math.clamp(hover.ground(), lowest, highest);
            int x = gaugeLeft + (clamped - lowest) * (gaugeWidth - 1) / (highest - lowest);
            graphics.fill(x, gaugeTop - 2, x + 1, gaugeTop + GAUGE_H + 2, 0xFFFFFFFF);
        }
    }

    void texts(GuiGraphics graphics, Font font, MapCamera camera, int lowest, int highest,
               @Nullable MapPick.Hit hover) {
        Component readout = hover == null ? Component.empty() : hover.known()
                ? Component.translatable(UI + "readout", hover.x(), hover.ground(), hover.z())
                : Component.translatable(UI + "unknown");
        graphics.drawString(font, readout, left + 6, top + 6, PanelKit.TEXT, false);
        if (lowest != MapSurface.NO_HEIGHT) {
            graphics.drawString(font, Component.translatable(UI + "height", lowest, highest), left + 6, top + 18,
                    PanelKit.LEGEND, false);
        }
        if (camera.plan()) {
            graphics.drawString(font, Component.translatable(UI + "contour", MapContours.MINOR_STEP,
                    MapContours.MAJOR_STEP), left + 6, top + 30, PanelKit.LEGEND, false);
        }
        for (int kind = MapMark.PENNED; kind <= MapMark.SPIRIT; kind++) {
            int x = left + 6 + (kind - 1) % 2 * 66;
            int y = top + 44 + (kind - 1) / 2 * 12;
            graphics.drawString(font, Component.translatable(UI + "mark." + MARK_KEYS[kind]), x + 8, y - 1,
                    PanelKit.LEGEND, false);
        }
    }
}
