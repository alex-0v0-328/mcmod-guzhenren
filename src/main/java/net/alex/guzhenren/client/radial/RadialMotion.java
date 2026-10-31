package net.alex.guzhenren.client.radial;

/**
 * The radial menu's [扇形选择器] animation, as pure functions of milliseconds so plain JUnit pins the timeline.
 *
 * <p>Opening: the ring grows from {@link #OPEN_FROM} to full size with a slight overshoot ({@link #getOpenScale})
 * while it fades in ({@link #getOpenAlpha}), and each sector appears {@link #STAGGER_MS} after the one before it,
 * clockwise ({@link #getAppear}). Pointing: a sector's hover weight eases toward 1 while pointed at and back to 0
 * after ({@link #approach}); it pushes the sector out, widens its rim and enlarges its icon. The name at the center
 * rises in over {@link #LABEL_MS} ({@link #getLabelIn}). Leaving: a pick flashes the chosen sector white
 * ({@link #getFlash}), swells the ring to {@link #PICK_TO} and fades it after a short hold; a cancel shrinks it to
 * {@link #CANCEL_TO} while it fades ({@link #getExitScale}, {@link #getExitAlpha}, {@link #isExitDone}).
 *
 * <p>{@link #mix} and {@link #fade} work on ARGB colors, channel by channel.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

public final class RadialMotion {

    public static final int OPEN_MS = 140;
    public static final int FADE_IN_MS = 100;
    public static final int STAGGER_MS = 30;
    public static final int APPEAR_MS = 120;
    public static final int LABEL_MS = 100;
    public static final int FLASH_MS = 90;
    public static final int PICK_GROW_MS = 180;
    public static final int PICK_HOLD_MS = 60;
    public static final int PICK_FADE_MS = 160;
    public static final int CANCEL_MS = 120;
    public static final float OPEN_FROM = 0.6F;
    public static final float PICK_TO = 1.08F;
    public static final float CANCEL_TO = 0.8F;
    private static final double HOVER_RATE = 14.0D;
    private static final double BACK = 1.70158D;

    private RadialMotion() {}

    public static float getOpenScale(long ms) {
        return OPEN_FROM + (1.0F - OPEN_FROM) * easeOutBack(getProgress(ms, OPEN_MS));
    }

    public static float getOpenAlpha(long ms) { return easeOutCubic(getProgress(ms, FADE_IN_MS)); }

    public static float getAppear(long ms, int index) {
        return easeOutCubic(getProgress(ms - (long) index * STAGGER_MS, APPEAR_MS));
    }

    public static float getLabelIn(long ms) { return easeOutCubic(getProgress(ms, LABEL_MS)); }

    public static float getFlash(long ms) { return 1.0F - getProgress(ms, FLASH_MS); }

    public static float getExitScale(long ms, boolean picked) {
        return picked ? 1.0F + (PICK_TO - 1.0F) * easeOutCubic(getProgress(ms, PICK_GROW_MS))
                : 1.0F - (1.0F - CANCEL_TO) * easeOutCubic(getProgress(ms, CANCEL_MS));
    }

    public static float getExitAlpha(long ms, boolean picked) {
        return picked ? 1.0F - getProgress(ms - PICK_HOLD_MS, PICK_FADE_MS) : 1.0F - getProgress(ms, CANCEL_MS);
    }

    public static boolean isExitDone(long ms, boolean picked) {
        return ms >= (picked ? PICK_HOLD_MS + PICK_FADE_MS : CANCEL_MS);
    }

    public static float approach(float current, float target, float seconds) {
        return current + (target - current) * (float) Math.min(1.0D, seconds * HOVER_RATE);
    }

    public static int mix(int from, int to, float amount) {
        int color = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = from >>> shift & 0xFF;
            int b = to >>> shift & 0xFF;
            color |= Math.round(a + (b - a) * amount) << shift;
        }
        return color;
    }

    public static int fade(int color, float alpha) {
        return Math.round((color >>> 24) * alpha) << 24 | color & 0xFFFFFF;
    }

    static float getProgress(long ms, int duration) {
        return ms <= 0L ? 0.0F : Math.min(1.0F, ms / (float) duration);
    }

    static float easeOutCubic(float x) {
        float rest = 1.0F - x;
        return 1.0F - rest * rest * rest;
    }

    static float easeOutBack(float x) {
        double rest = x - 1.0D;
        return (float) (1.0D + (BACK + 1.0D) * rest * rest * rest + BACK * rest * rest);
    }
}
