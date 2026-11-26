package net.alex.guzhenren.client.screen;

import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
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
 *   <li>真元海共有 20 种境界配色，色号按 {@code (转 - 1) × 4 + 阶} 计算（初 0、中 1、高 2、巅峰 3），每种配色的四
 *       阶亮度分别乘到海体四个明暗层上；该配色表仅在本类使用，保留在本地而不放入 {@code ModPalette}；未开窍或六转
 *       及以上仅绘制暗底与窗框。</li>
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
    private static final int STAGES = 4;
    private static final long FRAME_MS = 150L;
    private static final ResourceLocation BACK = sprite("back");
    private static final ResourceLocation FRONT = sprite("front");
    private static final Map<Talent, ResourceLocation> SEAS = new EnumMap<>(Map.of(
            Talent.TEN_EXTREMES, sprite("sea_ten_extremes"),
            Talent.FIRST, sprite("sea_jia"),
            Talent.SECOND, sprite("sea_yi"),
            Talent.THIRD, sprite("sea_bing"),
            Talent.FOURTH, sprite("sea_ding")));
    private static final int[][] SEA_COLORS = {
            { 0xFF123D2C, 0xFF1F7A4A, 0xFF3FB86A, 0xFFA8F0B0 },
            { 0xFF1C3330, 0xFF3A6650, 0xFF5F9470, 0xFFB6D8A8 },
            { 0xFF0E2A26, 0xFF1D5238, 0xFF2F7D4C, 0xFF86C48A },
            { 0xFF0A1A1C, 0xFF163A30, 0xFF245A42, 0xFF5F9A74 },
            { 0xFF5A2234, 0xFFB3474E, 0xFFE8806E, 0xFFFFD0B0 },
            { 0xFF4A1028, 0xFFA82434, 0xFFE04A3E, 0xFFFFB08A },
            { 0xFF36081E, 0xFF7E1828, 0xFFB62E2E, 0xFFF07E62 },
            { 0xFF22061A, 0xFF541222, 0xFF86202A, 0xFFC45848 },
            { 0xFF4A4C5E, 0xFF80869A, 0xFFAAB0BE, 0xFFE2E6EA },
            { 0xFF4E5470, 0xFF8A94AC, 0xFFBCC4D2, 0xFFF2F4F6 },
            { 0xFF5A6680, 0xFF9CACC2, 0xFFD4DEE8, 0xFFFFFFFF },
            { 0xFF6C7A92, 0xFFB8C6D6, 0xFFEAF0F6, 0xFFFFFFFF },
            { 0xFF6A4E2C, 0xFFB0924E, 0xFFDCC27C, 0xFFFFF2C4 },
            { 0xFF7A4A14, 0xFFD0942A, 0xFFF4CC4A, 0xFFFFF6B0 },
            { 0xFF6E3410, 0xFFC07A1A, 0xFFECAC2E, 0xFFFFE48A },
            { 0xFF7A2C0C, 0xFFD2781A, 0xFFFFBC22, 0xFFFFF08A },
            { 0xFF4C3A6E, 0xFF8A70B0, 0xFFBCA2DC, 0xFFF0E2FF },
            { 0xFF5A1E5E, 0xFFA2449E, 0xFFD070C8, 0xFFFFC4EC },
            { 0xFF22124E, 0xFF4A2A8A, 0xFF7446B8, 0xFFC49AF0 },
            { 0xFF2A1C74, 0xFF5A34C8, 0xFF9662FF, 0xFFE4CCFF },
    };

    private static ResourceLocation sprite(String name) { return Guzhenren.id("aperture/" + name); }

    static int getScale(int width, int height) {
        return Math.clamp(Math.min(width, height) / SIZE, MIN_SCALE, MAX_SCALE);
    }

    static void draw(SpriteBatch sprites, Aperture aperture, int x, int y, int scale) {
        int size = SIZE * scale;
        sprites.blit(BACK, x, y, size, size);
        ResourceLocation sea = SEAS.get(aperture.getTalent());
        int color = getColorIndex(aperture);
        if (sea != null && color >= 0) {
            int frame = (int) (Util.getMillis() / FRAME_MS % FRAMES);
            for (int layer = 0; layer < LAYERS; layer++) {
                sprites.blitRegion(sea, SIZE * FRAMES, SIZE * LAYERS, frame * SIZE, layer * SIZE, SIZE, SIZE, x, y,
                        size, size, SEA_COLORS[color][layer]);
            }
        }
        sprites.blit(FRONT, x, y, size, size);
    }

    private static int getColorIndex(Aperture aperture) {
        Rank rank = aperture.rank();
        if (rank == Rank.NONE || rank.compareTo(Rank.FIVE) > 0 || aperture.stage() == Stage.NONE) return -1;

        return (rank.ordinal() - Rank.ONE.ordinal()) * STAGES + aperture.stage().ordinal() - Stage.INITIAL.ordinal();
    }
}
