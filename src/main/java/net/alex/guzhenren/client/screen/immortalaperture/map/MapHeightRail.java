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
 * 从右缘往左伸的横线，一律 {@value #SHORT} 像素，线下 1 像素暗投影；光标处地面所在那条最长（{@value #HOVERED}）、变白，
 * 左边一枚 {@code chip} 写地面高，上下各两条依次 12、8 像素，再往外回到 {@value #SHORT}（中间长、两侧渐短）。
 * <ul>
 *   <li>线距不到 {@value #MIN_GAP} 像素时步长翻倍。</li>
 *   <li>从世界底层起每 {@value #MARK_STEP} 格（维度底高恒为 16 的倍数，即绝对高度 16 的倍数）一枚小标：刻度线左侧带投影的
 *       暗字，贴着该线线头；标距不到 {@value #MARK_MIN_GAP} 像素时标距翻倍，离悬停 {@code chip} 不到 {@value #MARK_CLEAR}
 *       像素的让开。</li>
 *   <li>上下端由 {@link MapPage} 给（视角簇下方到图例框上方），不画底板、不吃点击。</li>
 * </ul>
 */

final class MapHeightRail {

    private static final int STEP = 4;
    private static final int MIN_GAP = 3;
    private static final int SHORT = 4;
    private static final int HOVERED = 20;
    private static final int[] NEAR = {HOVERED, 12, 8};
    private static final int MARK_STEP = 16;
    private static final int MARK_MIN_GAP = 10;
    private static final int MARK_CLEAR = 13;
    private static final int MARK_GAP = 2;
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

    private int doubled(int step, int minGap, int lowest, int highest) {
        while ((long) (bottom - top) * step / (highest - lowest) < minGap) step *= 2;
        return step;
    }

    private int step(int lowest, int highest) { return doubled(STEP, MIN_GAP, lowest, highest); }

    private int y(int level, int lowest, int highest) {
        return bottom - (int) ((long) (level - lowest) * (bottom - top) / (highest - lowest));
    }

    private static int nearest(int ground, int step) { return Math.round(ground / (float) step) * step; }

    private static @Nullable Integer hovered(@Nullable MapPick.Hit hover) {
        return hover != null && hover.known() ? hover.ground() : null;
    }

    private static @Nullable Integer lit(int lowest, int highest, int step, @Nullable MapPick.Hit hover) {
        Integer ground = hovered(hover);
        return ground == null ? null : nearest(Math.clamp(ground, lowest, highest), step);
    }

    private static int distance(int level, int step, @Nullable Integer lit) {
        return lit == null ? Integer.MAX_VALUE : Math.abs(level - lit) / step;
    }

    private static int length(int distance) { return distance < NEAR.length ? NEAR[distance] : SHORT; }

    void fills(GuiGraphics graphics, int lowest, int highest, @Nullable MapPick.Hit hover) {
        if (!shown(lowest, highest)) return;

        int step = step(lowest, highest);
        Integer lit = lit(lowest, highest, step, hover);
        for (int level = Math.ceilDiv(lowest, step) * step; level <= highest; level += step) {
            int distance = distance(level, step, lit);
            int length = length(distance);
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
        if (!shown(lowest, highest)) return;

        marks(graphics, font, lowest, highest, hover);
        Integer ground = hovered(hover);
        if (ground == null) return;

        String text = Integer.toString(ground);
        PanelDrawing.right(graphics, font, text, right - HOVERED - 7, labelY(lowest, highest, ground) + 4,
                PanelDrawing.TEXT);
    }

    private void marks(GuiGraphics graphics, Font font, int lowest, int highest, @Nullable MapPick.Hit hover) {
        int step = step(lowest, highest);
        Integer lit = lit(lowest, highest, step, hover);
        int chipY = lit == null ? 0 : y(lit, lowest, highest);
        int every = doubled(MARK_STEP, MARK_MIN_GAP, lowest, highest);
        for (int level = Math.ceilDiv(lowest, every) * every; level <= highest; level += every) {
            int y = y(level, lowest, highest);
            if (lit != null && Math.abs(y - chipY) < MARK_CLEAR) continue;

            String text = Integer.toString(level);
            int textRight = right - length(distance(level, step, lit)) - MARK_GAP;
            graphics.drawString(font, text, textRight - font.width(text), y - 4, LINE, true);
        }
    }
}
