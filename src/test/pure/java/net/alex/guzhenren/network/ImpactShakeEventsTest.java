package net.alex.guzhenren.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpactShakeEventsTest {

    private static final float EPS = 1.0E-5F;

    @Test
    @DisplayName("a plunge shakes 0.25 degrees at impact power two, 0.1 more each power, 0.65 at six")
    void plungeStrengthGrowsWithPower() {
        assertEquals(0.25F, ImpactShakeEvents.getPlungeStrength(2.0F), EPS);
        assertEquals(0.45F, ImpactShakeEvents.getPlungeStrength(4.0F), EPS);
        assertEquals(0.65F, ImpactShakeEvents.getPlungeStrength(6.0F), EPS);
    }

    @Test
    @DisplayName("a plunge is felt 8 blocks plus 4 a power out: 16 at two, 32 at six")
    void plungeRadiusGrowsWithPower() {
        assertEquals(16.0D, ImpactShakeEvents.getPlungeRadius(2.0F), 1.0E-9D);
        assertEquals(32.0D, ImpactShakeEvents.getPlungeRadius(6.0F), 1.0E-9D);
    }

    @Test
    @DisplayName("the felt strength falls linearly from full at the impact to nothing at the radius")
    void falloffIsLinear() {
        assertEquals(0.75F, ImpactShakeEvents.getFalloff(0.75F, 0.0D, 16.0D), EPS);
        assertEquals(0.375F, ImpactShakeEvents.getFalloff(0.75F, 8.0D, 16.0D), EPS);
        assertEquals(0.0F, ImpactShakeEvents.getFalloff(0.75F, 16.0D, 16.0D), EPS);
        assertEquals(0.0F, ImpactShakeEvents.getFalloff(0.75F, 40.0D, 16.0D), EPS);
    }
}
