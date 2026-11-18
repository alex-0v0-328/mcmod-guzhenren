package net.alex.guzhenren.client.camera;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatCameraRigTest {

    private static final float EPS = 1.0E-5F;

    @Test
    @DisplayName("charging pulls the camera to 0.8 and full throttle pushes it out by 0.3")
    void distanceTargetFollowsChargeAndThrottle() {
        assertEquals(1.0F, CombatCameraRig.getDistanceTarget(false, 0.0F), EPS);
        assertEquals(0.8F, CombatCameraRig.getDistanceTarget(true, 0.0F), EPS);
        assertEquals(1.3F, CombatCameraRig.getDistanceTarget(false, 1.0F), EPS);
        assertEquals(1.15F, CombatCameraRig.getDistanceTarget(false, 0.5F), EPS);
        assertEquals(1.3F, CombatCameraRig.getDistanceTarget(false, 2.0F), EPS);
    }

    @Test
    @DisplayName("the shoulder offset starts closing past 50 degrees of pitch and is gone at 80")
    void squeezeClosesBetweenFiftyAndEighty() {
        assertEquals(0.0F, CombatCameraRig.getSqueeze(50.0F), EPS);
        assertEquals(0.5F, CombatCameraRig.getSqueeze(65.0F), EPS);
        assertEquals(0.5F, CombatCameraRig.getSqueeze(-65.0F), EPS);
        assertEquals(1.0F, CombatCameraRig.getSqueeze(80.0F), EPS);
        assertEquals(1.0F, CombatCameraRig.getSqueeze(90.0F), EPS);
    }

    @Test
    @DisplayName("one half-life covers half the gap")
    void approachHalvesTheGapPerHalfLife() {
        assertEquals(0.9F, CombatCameraRig.approach(1.0F, 0.8F, 0.15F, 0.15F), EPS);
        assertEquals(0.85F, CombatCameraRig.approach(1.0F, 0.8F, 0.3F, 0.15F), EPS);
        assertEquals(1.0F, CombatCameraRig.approach(1.0F, 0.8F, 0.0F, 0.15F), EPS);
        assertEquals(0.8F, CombatCameraRig.approach(1.0F, 0.8F, 0.1F, 0.0F), EPS);
    }

    @Test
    @DisplayName("the dash kick starts full, halves every 0.12 seconds and ends after a second")
    void kickDecaysAndEnds() {
        assertEquals(1.0F, CombatCameraRig.getKick(0.0F), EPS);
        assertEquals(0.5F, CombatCameraRig.getKick(0.12F), EPS);
        assertEquals(0.25F, CombatCameraRig.getKick(0.24F), EPS);
        assertEquals(0.0F, CombatCameraRig.getKick(1.0F), EPS);
        assertEquals(0.0F, CombatCameraRig.getKick(-0.1F), EPS);
    }
}
