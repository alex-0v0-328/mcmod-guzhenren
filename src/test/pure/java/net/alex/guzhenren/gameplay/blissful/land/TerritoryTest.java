package net.alex.guzhenren.gameplay.blissful.land;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoryTest {

    private static final Territory FOX = Territory.whole(612, 32);

    @Test
    @DisplayName("the whole Fox land is 1224 chunks a side, four acres a chunk")
    void wholeLandArea() {
        assertEquals(-612, FOX.minChunkX());
        assertEquals(611, FOX.maxChunkX());
        assertEquals(1224L * 1224L, FOX.chunks());
        assertEquals(5_992_704L, FOX.acres());
    }

    @Test
    @DisplayName("ceding a side drops that strip from the land and marks it ceded, outside stays plain void")
    void cedingAStrip() {
        Territory ceded = FOX.cede(Territory.Side.SOUTH, 8);
        assertEquals(603, ceded.maxChunkZ());
        assertEquals(1224L * 1216L * 4L, ceded.acres());
        assertTrue(ceded.isCeded(0, 604));
        assertFalse(ceded.isCeded(0, 603));
        assertFalse(ceded.isCeded(0, 612), "beyond the original square is not ceded, it never was land");
        assertFalse(ceded.contains(0, 604));
    }

    @Test
    @DisplayName("the centre 64 by 64 chunks can never be ceded, but any width up to it is allowed")
    void coreIsProtected() {
        assertEquals(580, FOX.maxCede(Territory.Side.WEST));
        assertTrue(FOX.canCede(Territory.Side.WEST, 580));
        assertFalse(FOX.canCede(Territory.Side.WEST, 581));
        assertFalse(FOX.canCede(Territory.Side.EAST, 0));
        Territory west = FOX.cede(Territory.Side.WEST, 580);
        assertEquals(-32, west.minChunkX());
        assertEquals(0, west.maxCede(Territory.Side.WEST));
        assertTrue(west.contains(-32, 0));
        Territory east = FOX.cede(Territory.Side.EAST, 580);
        assertEquals(31, east.maxChunkX());
        assertThrows(IllegalArgumentException.class, () -> east.cede(Territory.Side.EAST, 1));
    }

    @Test
    @DisplayName("cessions add up on the same side")
    void cessionsAccumulate() {
        Territory twice = FOX.cede(Territory.Side.NORTH, 10).cede(Territory.Side.NORTH, 5);
        assertEquals(15, twice.north());
        assertEquals(-597, twice.minChunkZ());
        assertEquals(565, twice.maxCede(Territory.Side.NORTH));
    }
}
