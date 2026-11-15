package net.alex.guzhenren;

import net.alex.guzhenren.effect.timed.MaliciousThoughtGuEffect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MaliciousThoughtGuEffectTest {

    @Test
    @DisplayName("the duration is 240 ticks -- twelve real seconds at 20 ticks each")
    void duration() {
        assertEquals(240, MaliciousThoughtGuEffect.DURATION_TICKS);
    }
}
