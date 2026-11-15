package net.alex.guzhenren.client.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SecondStageEventsTest {

    @Test
    @DisplayName("full FOV effects widen the view by 15 percent at full throttle")
    void fullEffectsWidenByFifteenPercent() {
        assertEquals(1.15F, SecondStageEvents.getFovFactor(1.0D, 1.0D), 1.0E-6F);
    }

    @Test
    @DisplayName("FOV effects off means no widening at all")
    void effectsOffMeansNoWidening() {
        assertEquals(1.0F, SecondStageEvents.getFovFactor(0.0D, 1.0D), 1.0E-6F);
    }

    @Test
    @DisplayName("the slider eases the widening linearly")
    void sliderEasesLinearly() {
        assertEquals(1.075F, SecondStageEvents.getFovFactor(0.5D, 1.0D), 1.0E-6F);
    }

    @Test
    @DisplayName("the widening follows the throttle")
    void wideningFollowsTheThrottle() {
        assertEquals(1.05F, SecondStageEvents.getFovFactor(1.0D, 1.0D / 3.0D), 1.0E-6F);
        assertEquals(1.0F, SecondStageEvents.getFovFactor(1.0D, 0.0D), 1.0E-6F);
    }
}
