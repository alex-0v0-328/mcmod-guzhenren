package net.alex.guzhenren.client.screen.blissful.map;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapPlotsTest {

    private static final Territory LAND = Territory.whole(10, 2);

    private static MapPlots.Plot field(int chunkX, int chunkZ) {
        return new MapPlots.Plot(MapPlots.Kind.FIELD, chunkX, chunkZ, 0, 0, 0, 0);
    }

    private static MapPlots.Plot planned(int chunkX, int chunkZ) {
        return new MapPlots.Plot(MapPlots.Kind.PLANNED, chunkX, chunkZ, 0, 0, 0, 0);
    }

    private static MapPlots.Plot pen(int index, int chunkX, int chunkZ) {
        return new MapPlots.Plot(MapPlots.Kind.PASTURE, chunkX, chunkZ, index, 0, 0, 0);
    }

    @Test
    @DisplayName("a pasture covers its 2×2 chunks, a field only its own chunk")
    void plotsCoverTheirChunks() {
        List<MapPlots.Plot> plots = List.of(field(3, 3), pen(0, -4, 5));
        assertEquals(MapPlots.Kind.FIELD, MapPlots.at(plots, 3, 3).kind());
        assertNull(MapPlots.at(plots, 4, 3));
        assertEquals(0, MapPlots.at(plots, -3, 6).index());
        assertNull(MapPlots.at(plots, -2, 6));
    }

    @Test
    @DisplayName("the pasture corner centres the 2×2 chunks on the cursor")
    void pastureCornerCentresOnCursor() {
        assertEquals(-1, MapPlots.pastureCorner(0.5));
        assertEquals(-1, MapPlots.pastureCorner(7.5));
        assertEquals(0, MapPlots.pastureCorner(8.5));
        assertEquals(-2, MapPlots.pastureCorner(-8.5));
        assertEquals(1, MapPlots.pastureCorner(31.0));
    }

    @Test
    @DisplayName("ceding a strip voids the fields, plans and pastures that touch it")
    void cedingCountsWhatTouchesTheStrip() {
        List<MapPlots.Plot> plots = List.of(field(9, 0), field(8, 0), planned(9, -10), pen(0, 7, 3), pen(1, 6, 3),
                field(-10, 0));
        assertEquals(new MapPlots.Voided(1, 1, 0), MapPlots.voided(plots, LAND, Territory.Side.EAST, 1));
        assertEquals(new MapPlots.Voided(2, 1, 1), MapPlots.voided(plots, LAND, Territory.Side.EAST, 2));
        assertEquals(new MapPlots.Voided(1, 0, 0), MapPlots.voided(plots, LAND, Territory.Side.WEST, 1));
        assertEquals(new MapPlots.Voided(0, 1, 0), MapPlots.voided(plots, LAND, Territory.Side.NORTH, 1));
        Territory ceded = LAND.cede(Territory.Side.EAST, 1);
        assertEquals(new MapPlots.Voided(1, 0, 1), MapPlots.voided(plots, ceded, Territory.Side.EAST, 1));
    }
}
