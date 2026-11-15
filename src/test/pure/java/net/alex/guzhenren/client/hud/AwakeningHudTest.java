package net.alex.guzhenren.client.hud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AwakeningHudTest {

    @Test
    @DisplayName("the plaque takes the largest whole scale up to three that fits 60% of the screen height")
    void plaqueScaleFitsTheScreen() {
        assertEquals(3, AwakeningHud.getPlaqueScale(360, 52));
        assertEquals(3, AwakeningHud.getPlaqueScale(360, 64));
        assertEquals(2, AwakeningHud.getPlaqueScale(240, 64));
        assertEquals(1, AwakeningHud.getPlaqueScale(120, 64));
        assertEquals(1, AwakeningHud.getPlaqueScale(60, 64));
    }
}
