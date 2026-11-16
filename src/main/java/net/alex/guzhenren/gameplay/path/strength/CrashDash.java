package net.alex.guzhenren.gameplay.path.strength;

import java.util.function.DoubleBinaryOperator;

/**
 * 横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]、横冲直撞蛊[Charging Crash Gu]冲刺的距离与播放速度规格。
 * <ul>
 *   <li>不依赖游戏类与 Epic Fight，纯 JUnit 可测（{@code CrashGuEffect} 为 {@code MobEffect}，须引导注册表）；玩家身上
 *       的来源由 {@link CrashSources} 读出再交本类。</li>
 *   <li>{@link #getMultiple} 四源相加（Alex 定）：1 + √P / {@link #ROOT_POINTS_PER_MULTIPLE} + 两猪之力
 *       {@link #TWO_BOARS_BONUS} + 兽力冲刺加成（驰马 1）+ 全力以赴 {@link #ALL_OUT_BONUS}；P 为不含解放的发挥点数。</li>
 *   <li>{@link #getScale}：Epic Fight 闪避坐标向量倍数 = {@link #COORD_SCALE} × 倍数，截到路程 {@link #MAX_TRAVEL} 格。</li>
 *   <li>{@link #STEP_TRAVEL}：冲刺片段倍数 1 时根骨骼位移（前、左、右 1.342 格，后 1.344 格），取值越过全部；
 *       {@link #getTravel} 得路径上限。</li>
 *   <li>{@link #getPlaySpeed}：片段播放速度 = 1 + 兽力播放速度加成（驰马 0.5）+ 全力以赴 {@link #ALL_OUT_SPEED_BONUS}，
 *       位移总量不变，片段 {@link #CLIP_TICKS} tick 按速度缩短（{@link #getClipTicks}）。</li>
 *   <li>{@link #limitSpeed} 再把本 tick 的播放速度压到位移 ≤ {@link #MAX_STEP} 格：原版专用服一个移动包超 10 格判移动过快
 *       拉回（单人、局域网房主豁免），多出的位移顺延到后续 tick；片段坐标由调用方以两时刻间距离给出。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashSources
 * @since 1.0.0
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
