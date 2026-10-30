package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.minecraft.util.Mth;

/**
 * The awakening ritual's [开窍仪式] clock, in ticks since the click: the server awakens at {@link #AWAKEN_TICKS},
 * reveals the aptitude [资质] at {@link #RELEASE_TICKS} and ends the ritual at {@link #END_TICKS}; every client draws
 * the ritual off the same marks.
 *
 * <p>Static curves, each a pure function of the elapsed ticks. The Hope Gu [希望蛊] flies out over {@link #FLY_TICKS},
 * hovers and bobs, then shrinks into the light from {@link #FADE_START_TICKS} and is gone at
 * {@link #FLASH_START_TICKS}. The aptitude bar over the hotbar climbs from the lowest grade's floor to the rolled base
 * over {@link #BAR_FILL_TICKS}, then holds full until the flash. The slow white flash [闪白] rises from
 * {@link #FLASH_START_TICKS} to full white at the awakening and fades out by the release; {@link #getFlashTint} is
 * how far a Ten-Extremes flash has turned pale goose yellow [淡鹅黄], full at the awakening and held while it fades.
 * At the release the player may move again and the aptitude plaques [资质牌匾] fade in, hold, and fade out by
 * {@link #END_TICKS}.
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
    public static final int FLASH_START_TICKS = 4 * Ticks.SECOND;
    public static final int AWAKEN_TICKS = 90;
    public static final int RELEASE_TICKS = 5 * Ticks.SECOND;
    public static final int PLAQUE_FADE_TICKS = 6;
    public static final int PLAQUE_OUT_TICKS = 154;
    public static final int END_TICKS = 164;
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
        return 1.0F - smooth(progress(elapsed, FADE_START_TICKS, FLASH_START_TICKS));
    }
    //endregion

    //region the white flash and the plaque
    public static float getFlashAlpha(float elapsed) {
        if (elapsed < AWAKEN_TICKS) return smooth(progress(elapsed, FLASH_START_TICKS, AWAKEN_TICKS));
        return 1.0F - smooth(progress(elapsed, AWAKEN_TICKS, RELEASE_TICKS));
    }

    public static float getFlashTint(float elapsed) {
        return smooth(progress(elapsed, FLASH_START_TICKS, AWAKEN_TICKS));
    }

    public static float getPlaqueAlpha(float elapsed) {
        if (elapsed < RELEASE_TICKS || elapsed >= END_TICKS) return 0.0F;
        if (elapsed < PLAQUE_OUT_TICKS) return progress(elapsed, RELEASE_TICKS, RELEASE_TICKS + PLAQUE_FADE_TICKS);
        return 1.0F - progress(elapsed, PLAQUE_OUT_TICKS, END_TICKS);
    }
    //endregion

    //region the aptitude bar
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

    private static float smooth(float x) { return x * x * (3.0F - 2.0F * x); }

    private static float easeOut(float x) {
        float rest = 1.0F - x;
        return 1.0F - rest * rest * rest;
    }
}
