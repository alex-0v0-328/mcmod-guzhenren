package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapSurface;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页右缘的竖向高度刻度（Alex 定，参考 Memory Atlas 的高度轨）：上端是当前贴图最高、下端最低，每 {@value #STEP} 格一条
 * 从右缘往左伸的横线，8 的倍数略长、40 的倍数再长，线下 1 像素暗投影；光标处地面所在那条最长、变白，左边一枚 {@code chip}
 * 写地面高，相邻 {@value #LENS} 条按距离渐长（鱼眼）。
 * <ul>
 *   <li>线距不到 {@value #MIN_GAP} 像素时步长翻倍，长短照旧按 8 与 40 的倍数分。</li>
 *   <li>上下端由 {@link MapPage} 给（视角簇下方到图例框上方），不画底板、不吃点击。</li>
 * </ul>
 */

final class MapHeightRail {

    private static final int STEP = 4;
    private static final int MIN_GAP = 3;
    private static final int LENS = 3;
    private static final int[] LENS_GROWTH = {0, 8, 4, 2};
    private static final int SHORT = 4;
    private static final int MID = 7;
    private static final int LONG = 10;
    private static final int HOVERED = 22;
    private static final int SHADOW = 0x80000000;
    private static final int LINE = 0xFFC8C0B8;
    private static final int CHIP_H = 16;
    private int right;
    private int top;
    private int bottom;

    void layout(int width, int top, int bottom) {
        this.right = width - 4;
        this.top = top;
        this.bottom = bottom;
    }

    private boolean shown(int lowest, int highest) {
        return lowest != MapSurface.NO_HEIGHT && highest > lowest && bottom - top > 2 * MIN_GAP;
    }

    private int step(int lowest, int highest) {
        int step = STEP;
        while ((long) (bottom - top) * step / (highest - lowest) < MIN_GAP) step *= 2;
        return step;
    }

    private int y(int level, int lowest, int highest) {
        return bottom - (int) ((long) (level - lowest) * (bottom - top) / (highest - lowest));
    }

    private static int nearest(int ground, int step) { return Math.round(ground / (float) step) * step; }

    private static @Nullable Integer hovered(@Nullable MapPick.Hit hover) {
        return hover != null && hover.known() ? hover.ground() : null;
    }

    void fills(GuiGraphics graphics, int lowest, int highest, @Nullable MapPick.Hit hover) {
        if (!shown(lowest, highest)) return;

        int step = step(lowest, highest);
        Integer ground = hovered(hover);
        int lit = ground == null ? Integer.MIN_VALUE : nearest(Math.clamp(ground, lowest, highest), step);
        for (int level = Math.ceilDiv(lowest, step) * step; level <= highest; level += step) {
            int length = level % 40 == 0 ? LONG : level % 8 == 0 ? MID : SHORT;
            int distance = ground == null ? Integer.MAX_VALUE : Math.abs(level - lit) / step;
            if (distance == 0) length = HOVERED;
            else if (distance <= LENS) length += LENS_GROWTH[distance];
            int y = y(level, lowest, highest);
            graphics.fill(right - length, y + 1, right, y + 2, SHADOW);
            graphics.fill(right - length, y, right, y + 1, distance == 0 ? PanelDrawing.TEXT : LINE);
        }
    }

    private int labelY(int lowest, int highest, int ground) {
        int lit = nearest(Math.clamp(ground, lowest, highest), step(lowest, highest));
        return y(lit, lowest, highest) - CHIP_H / 2;
    }

    void sprites(SpriteBatch sprites, Font font, int lowest, int highest, @Nullable MapPick.Hit hover) {
        Integer ground = hovered(hover);
        if (!shown(lowest, highest) || ground == null) return;

        int width = font.width(Integer.toString(ground)) + 8;
        sprites.blit(PanelDrawing.CHIP, right - HOVERED - 3 - width, labelY(lowest, highest, ground), width, CHIP_H);
    }

    void texts(GuiGraphics graphics, Font font, int lowest, int highest, @Nullable MapPick.Hit hover) {
        Integer ground = hovered(hover);
        if (!shown(lowest, highest) || ground == null) return;

        String text = Integer.toString(ground);
        PanelDrawing.right(graphics, font, text, right - HOVERED - 7, labelY(lowest, highest, ground) + 4,
                PanelDrawing.TEXT);
    }
}
