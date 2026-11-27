package net.alex.guzhenren.gameplay.immortalaperture.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringRulesTest {

    @Test
    @DisplayName("a hosting round holds twelve spring productions of 64 stones, 768 a spring")
    void roundMatchesTheGroundRate() {
        assertEquals(12, SpringRules.PRODUCTIONS_PER_ROUND);
        assertEquals(64, SpringRules.STONES_PER_PRODUCTION);
        assertEquals(768, SpringRules.STONES_PER_SPRING_PER_ROUND);
    }

    @Test
    @DisplayName("stones scale with the spring count, the round is one minute so both totals agree")
    void totalsScaleWithSprings() {
        assertEquals(0L, SpringRules.stonesPerRound(0));
        assertEquals(768L, SpringRules.stonesPerRound(1));
        assertEquals(15_360L, SpringRules.stonesPerRound(20));
        assertEquals(SpringRules.stonesPerRound(20), SpringRules.stonesPerMinute(20));
    }

    @Test
    @DisplayName("a negative count pays nothing and a huge count does not overflow")
    void edgesStayInRange() {
        assertEquals(0L, SpringRules.stonesPerRound(-5));
        assertEquals(768L * Integer.MAX_VALUE, SpringRules.stonesPerRound(Integer.MAX_VALUE));
    }
}
