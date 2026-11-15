package net.alex.guzhenren.gameplay.flight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightGrantTest {

    @Test
    @DisplayName("the ladder: none grants nothing, flight only flight, the second stage both")
    void ladderGrantsInOrder() {
        assertFalse(FlightGrant.NONE.canFly());
        assertFalse(FlightGrant.NONE.canFlySecondStage());
        assertTrue(FlightGrant.FLIGHT.canFly());
        assertFalse(FlightGrant.FLIGHT.canFlySecondStage());
        assertTrue(FlightGrant.SECOND_STAGE.canFly());
        assertTrue(FlightGrant.SECOND_STAGE.canFlySecondStage());
    }

    @Test
    @DisplayName("strongest keeps the higher rung in either order and never climbs past it")
    void strongestKeepsTheHigherRung() {
        for (FlightGrant first : FlightGrant.values()) {
            for (FlightGrant second : FlightGrant.values()) {
                FlightGrant expected = first.ordinal() >= second.ordinal() ? first : second;
                assertEquals(expected, FlightGrant.getStrongest(first, second));
                assertEquals(expected, FlightGrant.getStrongest(second, first));
            }
        }
        assertEquals(FlightGrant.FLIGHT, FlightGrant.getStrongest(FlightGrant.FLIGHT, FlightGrant.FLIGHT));
    }

    @Test
    @DisplayName("none is the identity, so a player with no flying Gu stays grounded")
    void noneIsTheIdentity() {
        for (FlightGrant grant : FlightGrant.values()) {
            assertEquals(grant, FlightGrant.getStrongest(FlightGrant.NONE, grant));
        }
    }
}
