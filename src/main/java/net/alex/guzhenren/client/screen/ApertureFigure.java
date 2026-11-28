package net.alex.guzhenren.client.screen;

import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ApertureSea;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

/**
 * B 面板空窍页签中的空窍动态插图：按同一原点依次叠画窍内暗底、真元海体与窗框三层精灵图。
 * <ul>
 *   <li>精灵图位于 {@code textures/gui/sprites/aperture/}，由 Pixel Art 会话导出：{@code back} 与 {@code front} 各
 *       为 32×32 不染色图，五张海体图为 256×128（横向 8 帧 × 纵向 4 明暗层：深、中、浅、浪尖），纯白平涂且层间互不
 *       重叠，逐层乘以对应海色后拼成整片真元海。</li>
 *   <li>水位高度直接固定在海体贴图中并按资质[Aptitude]档位选图（十绝 100、甲 90、乙 70、丙 50、丁 30），不随当前
 *       剩余真元升降（由 Alex 选定）。</li>
 *   <li>真元海按境界取 {@link ApertureSea} 配色，四阶亮度分别乘到海体四个明暗层上；未开窍或六转及以上仅绘制暗底与
 *       窗框。</li>
 *   <li>海浪动画以 8 帧循环播放、每帧 150 ms，直接读取系统时钟推进，不随服务端掉刻卡顿。</li>
 *   <li>绘制缩放采用整数倍率，受上下限 {@link #MIN_SCALE} 与 {@link #MAX_SCALE} 约束（1 倍下浪高仅剩 1 像素起伏，
 *       由 Alex 选定至少 2 倍的优先级高于面板文字栏最小宽度）。</li>
 * </ul>
 */

final class ApertureFigure {

    private ApertureFigure() {}

    static final int SIZE = 32;
    private static final int MIN_SCALE = 2;
    private static final int MAX_SCALE = 3;
    private static final int FRAMES = 8;
    private static final int LAYERS = 4;
    private static final long FRAME_MS = 150L;
    private static final ResourceLocation BACK = sprite("back");
    private static final ResourceLocation FRONT = sprite("front");
    private static final Map<Talent, ResourceLocation> SEAS = new EnumMap<>(Map.of(
            Talent.TEN_EXTREMES, sprite("sea_ten_extremes"),
            Talent.FIRST, sprite("sea_jia"),
            Talent.SECOND, sprite("sea_yi"),
            Talent.THIRD, sprite("sea_bing"),
            Talent.FOURTH, sprite("sea_ding")));

    private static ResourceLocation sprite(String name) { return Guzhenren.id("aperture/" + name); }

    static int getScale(int width, int height) {
        return Math.clamp(Math.min(width, height) / SIZE, MIN_SCALE, MAX_SCALE);
    }

    static void draw(SpriteBatch sprites, Aperture aperture, int x, int y, int scale) {
        int size = SIZE * scale;
        sprites.blit(BACK, x, y, size, size);
        ResourceLocation sea = SEAS.get(aperture.getTalent());
        int[] colors = ApertureSea.getColors(aperture);
        if (sea != null && colors != null) {
            int frame = (int) (Util.getMillis() / FRAME_MS % FRAMES);
            for (int layer = 0; layer < LAYERS; layer++) {
                sprites.blitRegion(sea, SIZE * FRAMES, SIZE * LAYERS, frame * SIZE, layer * SIZE, SIZE, SIZE, x, y,
                        size, size, colors[layer]);
            }
        }
        sprites.blit(FRONT, x, y, size, size);
    }
}
