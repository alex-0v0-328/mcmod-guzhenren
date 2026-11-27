package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewLimiterTest {

    @Test
    @DisplayName("four view changes a second are taken, the fifth is dropped until the window rolls over")
    void fourPerSecond() {
        ViewLimiter limiter = new ViewLimiter();
        for (int i = 0; i < 4; i++) assertTrue(limiter.tryAccept(100 + i));
        assertFalse(limiter.tryAccept(110), "the fifth inside the same second is ignored");
        assertFalse(limiter.tryAccept(119), "still the same window");
        assertTrue(limiter.tryAccept(120), "a new second opens a new window");
    }

    @Test
    @DisplayName("a reveal counts its chunks: one pasture fills a second, a field after it waits")
    void revealsCountChunks() {
        ViewLimiter limiter = new ViewLimiter();
        assertTrue(limiter.tryAccept(0, 4), "a 2 by 2 pasture takes the whole second");
        assertFalse(limiter.tryAccept(5, 1), "a field in the same second waits");
        assertTrue(limiter.tryAccept(20, 1));
        assertTrue(limiter.tryAccept(21, 3));
        assertFalse(limiter.tryAccept(22, 1));
    }

    @Test
    @DisplayName("a quiet spell resets the window, and dropped requests do not use it up")
    void quietResets() {
        ViewLimiter limiter = new ViewLimiter();
        for (int i = 0; i < 6; i++) limiter.tryAccept(0);
        assertTrue(limiter.tryAccept(500));
        assertTrue(limiter.tryAccept(501));
    }
}
