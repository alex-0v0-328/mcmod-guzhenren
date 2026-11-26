package net.alex.guzhenren.client.screen.blissful.map;

import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;
import net.alex.guzhenren.gameplay.blissful.map.MapTile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapColumnsTest {

    private static MapTile column(MapTerrain terrain, int ground, int surface, int flags) {
        short[] grounds = new short[MapTile.COLUMNS];
        short[] surfaces = new short[MapTile.COLUMNS];
        byte[] terrains = new byte[MapTile.COLUMNS];
        byte[] flag = new byte[MapTile.COLUMNS];
        grounds[0] = (short) ground;
        surfaces[0] = (short) surface;
        terrains[0] = terrain.id();
        flag[0] = (byte) flags;
        return new MapTile(0, 0, grounds, surfaces, terrains, flag);
    }

    @Test
    @DisplayName("a canopy, log or fence over water keeps its own terrain and height instead of turning into water")
    void raisedOverWater() {
        for (MapTerrain raised : new MapTerrain[] { MapTerrain.FOLIAGE, MapTerrain.WOOD, MapTerrain.FENCE }) {
            MapTile tile = column(raised, 62, 70, MapTile.FLAG_FLUID);
            assertEquals(raised, MapColumns.terrain(tile, 0));
            assertEquals(70, MapColumns.top(tile, 0));
        }
    }

    @Test
    @DisplayName("ground-level columns flagged as fluid are water, lava stays lava")
    void flatFluids() {
        assertEquals(MapTerrain.WATER, MapColumns.terrain(column(MapTerrain.GRASS, 62, 62, MapTile.FLAG_FLUID), 0));
        assertEquals(MapTerrain.WATER, MapColumns.terrain(column(MapTerrain.FLOWER, 62, 63, MapTile.FLAG_FLUID), 0));
        assertEquals(MapTerrain.LAVA, MapColumns.terrain(column(MapTerrain.LAVA, 40, 40, MapTile.FLAG_FLUID), 0));
        assertEquals(MapTerrain.FOLIAGE, MapColumns.terrain(column(MapTerrain.FOLIAGE, 64, 70, 0), 0));
    }
}
