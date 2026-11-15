package net.alex.guzhenren.client.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SonicBoomHudTest {

    private static final SonicBoomHud HUD = SonicBoomHud.INSTANCE;

    @AfterEach
    void forget() { HUD.reset(); }

    @Test
    @DisplayName("an entry this client asked for plays the ring from the tick the mark turns on")
    void requestedEntryPlays() {
        HUD.noteRequested(100);
        HUD.onSecondStageStarted(103);
        assertEquals(0.0F, HUD.getAge(103, 0.0F), 1.0E-6F);
        assertEquals(2.5F, HUD.getAge(105, 0.5F), 1.0E-6F);
    }

    @Test
    @DisplayName("a mark that turns on with no request, as a sync after a dimension change, plays nothing")
    void unrequestedEntryStaysSilent() {
        HUD.onSecondStageStarted(103);
        assertTrue(HUD.getAge(103, 0.0F) >= SonicBoomHud.LIFETIME_TICKS);
    }

    @Test
    @DisplayName("a request older than the window, or from a later player entity, plays nothing")
    void staleRequestStaysSilent() {
        HUD.noteRequested(100);
        HUD.onSecondStageStarted(121);
        assertTrue(HUD.getAge(121, 0.0F) >= SonicBoomHud.LIFETIME_TICKS);
        HUD.noteRequested(500);
        HUD.onSecondStageStarted(3);
        assertTrue(HUD.getAge(3, 0.0F) >= SonicBoomHud.LIFETIME_TICKS);
    }

    @Test
    @DisplayName("one request plays one ring")
    void requestIsSpentOnce() {
        HUD.noteRequested(100);
        HUD.onSecondStageStarted(101);
        HUD.onSecondStageStarted(110);
        assertEquals(1.0F, HUD.getAge(102, 0.0F), 1.0E-6F);
    }

    @Test
    @DisplayName("the frames play one per tick and hold the largest through the linger")
    void framesPlayOnePerTick() {
        assertEquals(0, SonicBoomHud.getFrame(0.0F));
        assertEquals(2, SonicBoomHud.getFrame(2.9F));
        assertEquals(4, SonicBoomHud.getFrame(4.0F));
        assertEquals(4, SonicBoomHud.getFrame(7.9F));
    }

    @Test
    @DisplayName("the ring stays full bright through the bloom, then fades to nothing by the end")
    void alphaFadesThroughTheLinger() {
        assertEquals(1.0F, SonicBoomHud.getAlpha(4.9F), 1.0E-6F);
        assertEquals(0.5F, SonicBoomHud.getAlpha(6.5F), 1.0E-6F);
        assertEquals(0.0F, SonicBoomHud.getAlpha(SonicBoomHud.LIFETIME_TICKS), 1.0E-6F);
    }

    @Test
    @DisplayName("the diameter opens from a quarter of the height past the screen width")
    void diameterOpensPastTheEdges() {
        assertEquals(90, SonicBoomHud.getDiameter(0.0F, 640, 360));
        assertEquals(1088, SonicBoomHud.getDiameter(SonicBoomHud.LIFETIME_TICKS, 640, 360));
        assertTrue(SonicBoomHud.getDiameter(4.0F, 640, 360) > (90 + 1088) / 2);
    }
}
