package net.alex.guzhenren.effect.timed;

/**
 * 横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]、横冲直撞蛊[Charging Crash Gu]冲刺距离规格。
 * <ul>
 *   <li>不依赖游戏类与 Epic Fight，纯 JUnit 可测（{@link CrashGuEffect} 为 {@code MobEffect}，须引导注册表）。</li>
 *   <li>{@link #COORD_SCALE}：Epic Fight 闪避坐标向量倍数，三蛊共用 4.5；{@link #getMultiple} 累加：
 *       全力以赴蛊[All-Out Effort Gu]期间 +1，兽力[Beast Strength]冲刺加成（驰马 1）再加、全力以赴时双倍；单独 4.5、
 *       任一 9.0、兼有 18.0。</li>
 *   <li>{@link #STEP_TRAVEL}：冲刺片段倍数 1 时根骨骼位移（前、左、右 1.342 格，后 1.344 格），取值越过全部；
 *       {@link #getTravel} 得路径上限约 6、12、24 格。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashGuEffect
 * @since 1.0.0
 */

public final class CrashDash {

    private CrashDash() {}

    public static final double COORD_SCALE = 4.5D;
    public static final double STEP_TRAVEL = 1.345D;

    public static double getMultiple(boolean allOut, double beastDashBonus) {
        double beast = Math.max(0.0D, beastDashBonus);
        return 1.0D + (allOut ? 1.0D + 2.0D * beast : beast);
    }

    public static double getScale(boolean allOut, double beastDashBonus) {
        return COORD_SCALE * getMultiple(allOut, beastDashBonus);
    }

    public static double getTravel(double scale) { return STEP_TRAVEL * scale; }
}
