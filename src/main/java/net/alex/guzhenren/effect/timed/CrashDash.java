package net.alex.guzhenren.effect.timed;

/**
 * The Crash Gu dash distance [横冲 / 直撞 / 横冲直撞], kept free of game classes so plain JUnit pins it ({@link CrashGuEffect}
 * is a {@code MobEffect}, which needs the registries bootstrapped to load). {@link #COORD_SCALE} is the multiplier on
 * Epic Fight's dodge coordinate vector: one number shared by the whole Crash Gu family, 4.5. {@link #getMultiple}
 * adds the doublings up: one more for All-Out Effort [全力以赴], and the held beasts' dash bonus (the galloping horse's
 * [驰马骏力] one) on top, twice over while All-Out Effort runs. So 4.5 alone, 9.0 with either, 18.0 with both.
 *
 * <p>{@link #STEP_TRAVEL} is how far the mod's dash clips carry the root at a scale of one: 1.342 blocks for
 * dash_forward, dash_left and dash_right, 1.344 for dash_backward, rounded up past them all, so {@link #getTravel}
 * bounds the dash's path from the scale: about 6, 12 and 24 blocks. It lives outside the EF bridge so the spec stays readable without Epic Fight on
 * the classpath.
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
