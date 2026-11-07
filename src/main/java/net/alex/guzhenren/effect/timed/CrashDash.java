package net.alex.guzhenren.effect.timed;

/**
 * The Crash Gu dash distance [横冲 / 直撞 / 横冲直撞], kept free of game classes so plain JUnit pins it ({@link CrashGuEffect}
 * is a {@code MobEffect}, which needs the registries bootstrapped to load). {@link #COORD_SCALE} is the multiplier on
 * Epic Fight's dodge coordinate vector: one number shared by the whole Crash Gu family, 4.5. While All-Out Effort
 * [全力以赴] runs, {@link #getScale} multiplies it by {@link #ALL_OUT_MULTIPLIER}, 9.0 for all three. It lives outside
 * the EF bridge so the spec stays readable without Epic Fight on the classpath.
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashGuEffect
 * @since 1.0.0
 */

public final class CrashDash {

    private CrashDash() {}

    public static final double COORD_SCALE = 4.5D;
    public static final double ALL_OUT_MULTIPLIER = 2.0D;

    public static double getScale(boolean allOut) { return allOut ? COORD_SCALE * ALL_OUT_MULTIPLIER : COORD_SCALE; }
}
