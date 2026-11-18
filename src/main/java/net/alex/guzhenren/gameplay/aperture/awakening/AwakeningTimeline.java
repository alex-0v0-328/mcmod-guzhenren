package net.alex.guzhenren.gameplay.aperture.awakening;

import java.util.Arrays;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.minecraft.util.Mth;

/**
 * 开窍[Awakening]仪式时间轴：定义右键起手后各阶段的关键帧时刻、插值曲线与逐 tick 像素档位。
 * <ul>
 *   <li>服务端在 {@link #AWAKEN_TICKS} 开窍、{@link #RELEASE_TICKS} 揭晓资质并解除定身、{@link #END_TICKS} 结束仪式；客
 *       户端按同一套时刻驱动表现。</li>
 *   <li>快捷栏上方的资质条在 {@link #BAR_FILL_TICKS} 内从最低档下限爬升至预掷基数，满格停驻至闪白开始。</li>
 *   <li>希望蛊[Hope Gu]在 {@link #FLY_TICKS} 内缓动飞至悬停点并上下浮动；燃烬帧 {@code 0} 为原物品图标，按
 *       {@code BURNOUT_STEP_TICKS} 逐级切至第 7 帧，到 {@link #FLASH_START_TICKS} 燃尽消失（返回 {@code -1}）。</li>
 *   <li>屏幕像素特效按整 tick 跳档、不做帧间插值：闪白菱形从 {@code 0}（不画）外扩至 {@link #FLASH_STEPS} 盖满全屏后再
 *       收回；资质牌匾自中线上下展开（展满高度即停），经两次斜向扫光（返回斜带左上沿 {@code x + y}，不扫时为
 *       {@code -1}），最后从 {@link #SHATTER_TICKS} 起自下而上按每行 {@link #SHATTER_BLOCK} 像素逐帧碎裂。</li>
 * </ul>
 */

public final class AwakeningTimeline {

    private AwakeningTimeline() {}

    public static final int FLY_TICKS = 12;
    public static final int FLASH_START_TICKS = 4 * Ticks.SECOND;
    public static final int AWAKEN_TICKS = 90;
    public static final int RELEASE_TICKS = 5 * Ticks.SECOND;
    public static final int SHATTER_TICKS = 154;
    public static final int END_TICKS = 164;
    public static final int BAR_FILL_TICKS = 3 * Ticks.SECOND;
    public static final int FLASH_STEPS = 6;
    public static final int SHATTER_BLOCK = 8;
    private static final int BAR_FLOOR = Talent.LOWEST.getMinPercent();
    private static final float BOB_RADIANS_PER_TICK = 0.15F;
    private static final float BOB_BLOCKS = 0.08F;
    private static final int[] BURNOUT_STEP_TICKS = {24, 36, 48, 60, 66, 72, 76};
    private static final int FLASH_STEP_TICKS = 2;
    private static final int FLASH_TURN_TICKS = AWAKEN_TICKS + FLASH_STEP_TICKS;
    private static final int UNFOLD_ROWS_PER_TICK = 16;
    private static final int[] SHEEN_START_TICKS = {110, 130};
    private static final int SHEEN_PIXELS_PER_TICK = 8;

    //region the Hope Gu's entrance [希望蛊入场]
    public static float getFlight(float elapsed) { return easeOut(progress(elapsed, 0, FLY_TICKS)); }

    public static float getBob(float elapsed) {
        return elapsed < FLY_TICKS ? 0.0F : Mth.sin((elapsed - FLY_TICKS) * BOB_RADIANS_PER_TICK) * BOB_BLOCKS;
    }

    public static int getBurnoutFrame(int tick) {
        if (tick >= FLASH_START_TICKS) return -1;
        int frame = 0;
        for (int step : BURNOUT_STEP_TICKS) {
            if (tick >= step) frame++;
        }
        return frame;
    }

    public static boolean isBurnoutStep(int tick) { return Arrays.binarySearch(BURNOUT_STEP_TICKS, tick) >= 0; }
    //endregion

    //region the white flash and the plaque [白闪与铭牌]
    public static int getFlashStep(int tick) {
        if (tick < FLASH_START_TICKS || tick >= RELEASE_TICKS) return 0;
        if (tick < FLASH_TURN_TICKS) return 1 + (tick - FLASH_START_TICKS) / FLASH_STEP_TICKS;
        return FLASH_STEPS - 1 - (tick - FLASH_TURN_TICKS) / FLASH_STEP_TICKS;
    }

    public static int getFlashReach(int step, int farthest) { return farthest * step / FLASH_STEPS; }

    public static boolean showsPlaque(int tick, int height) {
        return tick >= RELEASE_TICKS && tick < END_TICKS && getShatteredRows(tick) * SHATTER_BLOCK < height;
    }

    public static int getUnfoldedRows(int tick, int height) {
        if (tick < RELEASE_TICKS) return 0;
        return Math.min(height, (tick - RELEASE_TICKS + 1) * UNFOLD_ROWS_PER_TICK);
    }

    public static int getSheenOffset(int tick, int span) {
        for (int start : SHEEN_START_TICKS) {
            int offset = (tick - start) * SHEEN_PIXELS_PER_TICK;
            if (offset >= 0 && offset < span) return offset;
        }
        return -1;
    }

    public static int getShatteredRows(int tick) { return tick < SHATTER_TICKS ? 0 : tick - SHATTER_TICKS + 1; }
    //endregion

    //region the aptitude bar [资质条]
    public static boolean showsBar(float elapsed) { return elapsed >= 0.0F && elapsed < FLASH_START_TICKS; }

    public static boolean isBarFull(float elapsed) { return elapsed >= BAR_FILL_TICKS; }

    public static int getBarBase(float elapsed, int base) {
        return BAR_FLOOR + (int) ((base - BAR_FLOOR) * progress(elapsed, 0, BAR_FILL_TICKS));
    }
    //endregion

    public static boolean holdsStill(float elapsed) { return elapsed < RELEASE_TICKS; }

    public static boolean isOver(float elapsed) { return elapsed >= END_TICKS; }

    private static float progress(float elapsed, float from, float to) {
        return Mth.clamp((elapsed - from) / (to - from), 0.0F, 1.0F);
    }

    private static float easeOut(float x) {
        float rest = 1.0F - x;
        return 1.0F - rest * rest * rest;
    }
}
