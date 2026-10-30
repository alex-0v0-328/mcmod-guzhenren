package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.minecraft.util.Mth;

/**
 * The awakening ritual's [开窍仪式] clock, in ticks since the click: the server awakens at {@link #AWAKEN_TICKS}, and
 * every client draws the rest off the same marks.
 *
 * <p>Static curves, each a pure function of the elapsed ticks. The Hope Gu [希望蛊] flies out over {@link #FLY_TICKS},
 * hovers and bobs, then shrinks into the light from {@link #FADE_START_TICKS}. The white halo [白色光晕] rises from
 * nothing at the screen edges and closes in to full white at {@link #AWAKEN_TICKS}; it holds there until
 * {@link #RELEASE_TICKS}, when the player may move again, then opens from the center out to the edges and is gone at
 * {@link #CLEAR_TICKS}. The aptitude plaque [资质牌匾] fades in at the release, holds, and fades out by
 * {@link #END_TICKS}. The aptitude bar over the hotbar climbs from the lowest grade's floor to the rolled base over
 * {@link #BAR_FILL_TICKS}, then holds full until the awakening.
 *
 * <p>{@link #getHaloOpening} is the clear hole's size as a fraction of the screen: {@code 0} is full white, and at
 * {@link #OPEN_HALO} the screen sees only the faint inner half of the halo texture, at zero alpha to begin with.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @since 1.0.0
 */

public final class AwakeningTimeline {

    private AwakeningTimeline() {}

    public static final int FLY_TICKS = 12;
    public static final int FADE_START_TICKS = 68;
    public static final int AWAKEN_TICKS = 4 * Ticks.SECOND;
    public static final int RELEASE_TICKS = 92;
    public static final int CLEAR_TICKS = 6 * Ticks.SECOND;
    public static final int PLAQUE_FADE_TICKS = 6;
    public static final int PLAQUE_OUT_TICKS = 158;
    public static final int END_TICKS = 168;
    public static final float OPEN_HALO = 3.0F;
    public static final int BAR_FILL_TICKS = 3 * Ticks.SECOND;
    private static final int BAR_FLOOR = Talent.LOWEST.getMinPercent();
    private static final float BOB_RADIANS_PER_TICK = 0.15F;
    private static final float BOB_BLOCKS = 0.08F;

    //region the Hope Gu before the player
    public static float getFlight(float elapsed) { return easeOut(progress(elapsed, 0, FLY_TICKS)); }

    public static float getBob(float elapsed) {
        return elapsed < FLY_TICKS ? 0.0F : Mth.sin((elapsed - FLY_TICKS) * BOB_RADIANS_PER_TICK) * BOB_BLOCKS;
    }

    public static float getGuScale(float elapsed) {
        return 1.0F - smooth(progress(elapsed, FADE_START_TICKS, AWAKEN_TICKS));
    }
    //endregion

    //region the white halo and the plaque
    public static float getHaloOpening(float elapsed) {
        if (elapsed < AWAKEN_TICKS) return OPEN_HALO * (1.0F - smooth(progress(elapsed, FLY_TICKS, AWAKEN_TICKS)));
        if (elapsed < RELEASE_TICKS) return 0.0F;
        return OPEN_HALO * easeOut(progress(elapsed, RELEASE_TICKS, CLEAR_TICKS));
    }

    public static float getHaloAlpha(float elapsed) {
        if (elapsed < AWAKEN_TICKS) return smooth(progress(elapsed, FLY_TICKS, AWAKEN_TICKS));
        if (elapsed < RELEASE_TICKS) return 1.0F;
        return 1.0F - easeOut(progress(elapsed, RELEASE_TICKS, CLEAR_TICKS));
    }

    public static float getPlaqueAlpha(float elapsed) {
        if (elapsed < RELEASE_TICKS || elapsed >= END_TICKS) return 0.0F;
        if (elapsed < PLAQUE_OUT_TICKS) return progress(elapsed, RELEASE_TICKS, RELEASE_TICKS + PLAQUE_FADE_TICKS);
        return 1.0F - progress(elapsed, PLAQUE_OUT_TICKS, END_TICKS);
    }
    //endregion

    //region the aptitude bar
    public static boolean showsBar(float elapsed) { return elapsed >= 0.0F && elapsed < AWAKEN_TICKS; }

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

    private static float smooth(float x) { return x * x * (3.0F - 2.0F * x); }

    private static float easeOut(float x) {
        float rest = 1.0F - x;
        return 1.0F - rest * rest * rest;
    }
}
