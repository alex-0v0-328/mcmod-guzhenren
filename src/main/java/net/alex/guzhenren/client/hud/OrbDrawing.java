package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 状态 HUD 圆珠的液面画法：按圆窗逐行裁剪，液面下整片平涂液色，液面顶 4 行由波面遮罩（{@code hud/orb_wave}）乘色画出
 * 波浪，底部精炼部分换深色；珠框（{@code hud/orb_main} 32×32、{@code hud/orb_sub} 24×24，窗内各留 2 像素边）最后盖
 * 上。
 * <ul>
 *   <li>⚠ 液色用 {@link GuiGraphics#fill} 画，须在同帧的 {@link SpriteBatch} 之前调用 {@link #fillLiquid}，波面与珠框
 *       再由 {@link #drawWave}、{@link #drawFrame} 在批内画，层序才对。</li>
 *   <li>波面图横 8 帧 × 纵 3 层（浪尖、浅、体），每格 32×4、纯白、层间不重叠；每帧 150 ms，读系统时钟，不随服务端掉刻
 *       卡顿。</li>
 *   <li>{@code fromX}、{@code toX} 限定窗内列区间，两只空窍时主珠竖分两室各画各的液面。</li>
 * </ul>
 */

final class OrbDrawing {

    private OrbDrawing() {}

    static final int MAIN = 32;
    static final int SUB = 24;
    static final int INSET = 2;
    private static final ResourceLocation MAIN_FRAME = Guzhenren.id("hud/orb_main");
    private static final ResourceLocation SUB_FRAME = Guzhenren.id("hud/orb_sub");
    private static final ResourceLocation WAVE = Guzhenren.id("hud/orb_wave");
    static final int WAVE_W = 256;
    static final int WAVE_H = 12;
    private static final int WAVE_FRAME = 32;
    private static final int WAVE_ROWS = 4;
    private static final int WAVE_LAYERS = 3;
    private static final int FRAMES = 8;
    private static final long FRAME_MS = 150L;

    /** 液体四阶色：深（精炼）、体、浅、浪尖。 */
    record Liquid(int deep, int body, int light, int crest) {}

    /** 窗内直径 {@code window} 的圆在第 {@code row} 行覆盖的列区间 {@code [起, 止)}，打包成 {@code 起 << 16 | 止}。 */
    static int span(int window, int row) {
        double radius = window / 2.0;
        double dy = row + 0.5 - radius;
        int from = window;
        int to = 0;
        for (int column = 0; column < window; column++) {
            double dx = column + 0.5 - radius;
            if (dx * dx + dy * dy > radius * radius) continue;
            from = Math.min(from, column);
            to = column + 1;
        }
        return from << 16 | to;
    }

    static int surface(int window, float fraction) {
        return window - Math.round(window * Math.clamp(fraction, 0.0F, 1.0F));
    }

    static void fillLiquid(GuiGraphics graphics, int x, int y, int window, int fromX, int toX, float fraction,
                           float refined, Liquid liquid) {
        int top = surface(window, fraction) + WAVE_ROWS;
        int deepTop = surface(window, Math.min(refined, fraction));
        for (int row = top; row < window; row++) {
            int span = span(window, row);
            int from = Math.max(span >>> 16, fromX);
            int to = Math.min(span & 0xFFFF, toX);
            if (from >= to) continue;
            graphics.fill(x + from, y + row, x + to, y + row + 1, row >= deepTop && refined > 0.0F
                    ? liquid.deep() : liquid.body());
        }
    }

    static void drawWave(SpriteBatch sprites, int x, int y, int window, int fromX, int toX, float fraction,
                         Liquid liquid) {
        if (fraction <= 0.0F) return;
        int top = surface(window, fraction);
        int frame = (int) (Util.getMillis() / FRAME_MS % FRAMES);
        int[] tints = { liquid.crest(), liquid.light(), liquid.body() };
        for (int band = 0; band < WAVE_ROWS && top + band < window; band++) {
            int span = span(window, top + band);
            int from = Math.max(span >>> 16, fromX);
            int to = Math.min(span & 0xFFFF, toX);
            if (from >= to) continue;
            for (int layer = 0; layer < WAVE_LAYERS; layer++) {
                sprites.blitRegion(WAVE, WAVE_W, WAVE_H, frame * WAVE_FRAME + from % WAVE_FRAME,
                        layer * WAVE_ROWS + band, to - from, 1, x + from, y + top + band, to - from, 1, tints[layer]);
            }
        }
    }

    static void drawFrame(SpriteBatch sprites, int x, int y, boolean main) {
        int size = main ? MAIN : SUB;
        sprites.blit(main ? MAIN_FRAME : SUB_FRAME, x, y, size, size);
    }
}
