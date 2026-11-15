package net.alex.guzhenren.client.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactShakesTest {

    private static final float EPS = 1.0E-5F;

    @Test
    @DisplayName("a shake opens at its strength and fades on the square of the time left")
    void shakeFadesOnTheSquare() {
        assertEquals(0.75F, ImpactShakes.getShake(0.0F, 0.75F, 10), EPS);
        assertEquals(0.1875F, ImpactShakes.getShake(5.0F, 0.75F, 10), EPS);
        assertTrue(ImpactShakes.getShake(9.9F, 0.75F, 10) > 0.0F);
    }

    @Test
    @DisplayName("a shake is gone at its end and silent before its start")
    void shakeEndsAtItsLength() {
        assertEquals(0.0F, ImpactShakes.getShake(10.0F, 0.75F, 10), EPS);
        assertEquals(0.0F, ImpactShakes.getShake(-1.0F, 0.75F, 10), EPS);
        assertEquals(0.0F, ImpactShakes.getShake(15.0F, 0.65F, 15), EPS);
    }
}
