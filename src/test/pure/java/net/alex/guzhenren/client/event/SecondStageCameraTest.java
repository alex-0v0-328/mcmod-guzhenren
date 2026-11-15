package net.alex.guzhenren.client.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SecondStageCameraTest {

    private static final float EPS = 1.0E-5F;

    @Test
    @DisplayName("the roll banks 0.025 degrees per degree a second of turn at full throttle, capped at 8")
    void rollBanksIntoTheTurn() {
        assertEquals(4.5F, SecondStageCamera.getRollTarget(180.0F, 1.0F), EPS);
        assertEquals(-2.25F, SecondStageCamera.getRollTarget(-180.0F, 0.5F), EPS);
        assertEquals(8.0F, SecondStageCamera.getRollTarget(1000.0F, 1.0F), EPS);
        assertEquals(-8.0F, SecondStageCamera.getRollTarget(-1000.0F, 1.0F), EPS);
        assertEquals(0.0F, SecondStageCamera.getRollTarget(180.0F, 0.0F), EPS);
    }

    @Test
    @DisplayName("the cruise shake starts past 0.6 throttle and reaches 0.15 degrees at full")
    void cruiseShakeStartsPastSixTenths() {
        assertEquals(0.0F, SecondStageCamera.getCruiseShake(0.6F), EPS);
        assertEquals(0.075F, SecondStageCamera.getCruiseShake(0.8F), EPS);
        assertEquals(0.15F, SecondStageCamera.getCruiseShake(1.0F), EPS);
    }
}
