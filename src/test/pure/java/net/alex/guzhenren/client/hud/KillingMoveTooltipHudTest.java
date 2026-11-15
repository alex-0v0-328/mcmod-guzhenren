package net.alex.guzhenren.client.hud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillingMoveTooltipHudTest {

    private static final float EPS = 1.0E-3F;
    private static final long IN = KillingMoveTooltipHud.SLIDE_IN_MS;
    private static final long SHOWN = KillingMoveTooltipHud.SHOWN_MS;
    private static final long OUT = KillingMoveTooltipHud.SLIDE_OUT_MS;

    @Test
    @DisplayName("it slides in over 0.15 s, stays until 5 s after the cast, and is gone 0.25 s later")
    void slidesInStaysAndSlidesOut() {
        assertEquals(1.0F, KillingMoveTooltipHud.getOffset(0L, 0L), EPS);
        assertTrue(KillingMoveTooltipHud.getOffset(IN / 2, IN / 2) < 0.5F, "the slide-in does not ease out");
        assertEquals(0.0F, KillingMoveTooltipHud.getOffset(IN, IN), EPS);
        assertEquals(0.0F, KillingMoveTooltipHud.getOffset(SHOWN, SHOWN), EPS);
        assertTrue(KillingMoveTooltipHud.getOffset(SHOWN + OUT / 2, SHOWN + OUT / 2) < 0.5F,
                "the slide-out does not ease in");
        assertEquals(1.0F, KillingMoveTooltipHud.getOffset(SHOWN + OUT, SHOWN + OUT), EPS);
        assertFalse(KillingMoveTooltipHud.isDone(SHOWN + OUT - 1L));
        assertTrue(KillingMoveTooltipHud.isDone(SHOWN + OUT));
    }

    @Test
    @DisplayName("it fades only while it slides out")
    void fadesOnlyOnTheWayOut() {
        assertEquals(1.0F, KillingMoveTooltipHud.getAlpha(SHOWN), EPS);
        assertEquals(0.5F, KillingMoveTooltipHud.getAlpha(SHOWN + OUT / 2), EPS);
        assertEquals(0.0F, KillingMoveTooltipHud.getAlpha(SHOWN + OUT), EPS);
    }

    @Test
    @DisplayName("a cast while it leaves turns it back from where it is")
    void turnsBackWithoutJumping() {
        for (float offset : new float[] {0.0F, 0.3F, 0.8F, 1.0F}) {
            long elapsed = KillingMoveTooltipHud.getEnterElapsed(offset);
            assertEquals(offset, KillingMoveTooltipHud.getOffset(elapsed, 0L), 0.02F);
        }
    }
}
