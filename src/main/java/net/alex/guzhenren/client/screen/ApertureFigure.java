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
 * B 面板空窍[Aperture]页的空窍图：窍内暗底、海体、窗框三层同原点叠画。
 * <ul>
 *   <li>精灵图在 {@code textures/gui/sprites/aperture/}，由 Pixel Art 会话导出：{@code back}、{@code front} 各 32×32
 *       不染色；五张海体 256×128 = 横 8 帧 × 纵 4 明暗层（深、中、浅、浪尖），纯白平涂、层间不重叠，逐层乘色后拼成整片海。</li>
 *   <li>水位画死在海体图里，按资质选图（十绝 100、甲 90、乙 70、丙 50、丁 30），不随当前真元升降（Alex 定）。</li>
 *   <li>海色 20 种，色号 = (转 − 1) × 4 + 阶（初 0、中 1、高 2、巅峰 3），每色四阶对应海体四层；只此一处用，留本地不进
 *       {@code ModPalette}。未开窍、六转及以上只画暗底和窗框。</li>
 *   <li>8 帧循环、每帧 150 ms，按系统时钟走，不随服务端掉刻卡顿。</li>
 *   <li>按整数倍绘制，至少 {@link #MIN_SCALE} 倍（1 倍时浪只剩 1 像素起伏，Alex 定可比面板文字栏下限更优先），
 *       至多 {@link #MAX_SCALE} 倍。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
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

        return (rank.ordinal() - Rank.ONE.ordinal()) * STAGES + aperture.stage().ordinal() - Stage.INIT.ordinal();
    }
}
