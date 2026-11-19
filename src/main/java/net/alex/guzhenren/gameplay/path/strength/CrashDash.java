package net.alex.guzhenren.gameplay.path.strength;

import java.util.function.DoubleBinaryOperator;

/**
 * 横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]与横冲直撞蛊[Charging Crash Gu]的冲刺距离与动画播放速度纯函数
 * （不依赖 Minecraft 或 Epic Fight 类，支持裸 JVM 测试；玩家状态由 {@link CrashSources} 采集后传入）。
 * <ul>
 *   <li>距离倍率 {@link #getMultiple} 由四项相加（Alex 定）：{@code 1 + sqrt(P) / ROOT_POINTS_PER_MULTIPLE} + 两猪之力
 *       加成（{@link #TWO_BOARS_BONUS}）+ 兽力冲刺加成（如驰马 {@code 1.0}）+ 全力以赴加成（{@link #ALL_OUT_BONUS}），
 *       其中 {@code P} 为总力量点[Strength Points]（不吃解放、蓄力与苦力倍率）。</li>
 *   <li>{@link #getScale} 将距离倍率乘以 {@link #COORD_SCALE} 得到 Epic Fight 闪避动画根骨骼位移缩放系数，并将总路程
 *       （{@link #getTravel}，单倍片段根骨骼位移为 {@link #STEP_TRAVEL}）封顶于 {@link #MAX_TRAVEL}（32 格）。</li>
 *   <li>动画播放速度 {@link #getPlaySpeed} = {@code 1 + 兽力速度加成 + 全力以赴速度加成}（总路程不变，动画持续 tick 数
 *       {@link #getClipTicks} 随播放速度加快而缩短）。</li>
 *   <li>{@link #limitSpeed} 将单 tick 内的动画播放速度二分压低至单步位移不超过 {@link #MAX_STEP}（9 格），防止在专用服
 *       务器上因单个移动包位移超过 10 格触发原版「移动过快」回拉（压低的部分顺延到后续 tick 播完）。</li>
 * </ul>
 */

public final class CrashDash {

    private CrashDash() {}

    public static final double COORD_SCALE = 4.5D;
    public static final double STEP_TRAVEL = 1.345D;
    public static final double ROOT_POINTS_PER_MULTIPLE = 10.0D;
    public static final double TWO_BOARS_BONUS = 0.5D;
    public static final double ALL_OUT_BONUS = 1.0D;
    public static final double MAX_TRAVEL = 32.0D;
    public static final double ALL_OUT_SPEED_BONUS = 0.5D;
    public static final double MAX_STEP = 9.0D;
    public static final int CLIP_TICKS = 7;
    public static final float TICK_SECONDS = 0.05F;
    private static final int SEARCH_STEPS = 16;

    //region Distance [距离]
    public static double getMultiple(int points, boolean twoBoars, double beastDashBonus, boolean allOut) {
        return 1.0D + Math.sqrt(Math.max(0, points)) / ROOT_POINTS_PER_MULTIPLE
                + (twoBoars ? TWO_BOARS_BONUS : 0.0D) + Math.max(0.0D, beastDashBonus)
                + (allOut ? ALL_OUT_BONUS : 0.0D);
    }

    public static double getScale(double multiple) {
        return Math.min(COORD_SCALE * Math.max(1.0D, multiple), MAX_TRAVEL / STEP_TRAVEL);
    }

    public static double getTravel(double scale) { return STEP_TRAVEL * scale; }
    //endregion

    //region Speed [速度]
    public static double getPlaySpeed(double beastSpeedBonus, boolean allOut) {
        return 1.0D + Math.max(0.0D, beastSpeedBonus) + (allOut ? ALL_OUT_SPEED_BONUS : 0.0D);
    }

    public static int getClipTicks(double playSpeed) {
        return (int) Math.ceil(CLIP_TICKS / Math.max(1.0D, playSpeed) - 1.0E-9D);
    }

    public static float limitSpeed(float speed, float elapsed, double scale, DoubleBinaryOperator span) {
        float step = speed * TICK_SECONDS;
        if (!(step > 0.0F) || span.applyAsDouble(elapsed, elapsed + step) * scale <= MAX_STEP) return speed;

        float low = 0.0F;
        float high = step;
        for (int i = 0; i < SEARCH_STEPS; i++) {
            float middle = (low + high) / 2.0F;
            if (span.applyAsDouble(elapsed, elapsed + middle) * scale <= MAX_STEP) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low / TICK_SECONDS;
    }
    //endregion
}
