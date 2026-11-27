package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("a floating canopy keeps the ground under it solid, a trunk fills up to the canopy, a bush rests")
    void canopies() {
        MapTestTiles tiles = new MapTestTiles().fill(0, 0, 0, 0, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .tree(1, 1, 68, 70, false).tree(2, 2, 67, 71, true).tree(3, 3, 65, 66, false);
        MapTile tile = tiles.tile(0, 0);
        assertTrue(MapColumns.floats(tile, MapTile.index(1, 1)));
        assertEquals(64, MapColumns.solidTop(tile, MapTile.index(1, 1)));
        assertEquals(70, MapColumns.top(tile, MapTile.index(1, 1)));
        assertEquals(MapTerrain.GRASS, MapColumns.base(tile, MapTile.index(1, 1)));
        assertEquals(65, MapColumns.solidPlane(tiles, 1, 1, 0));
        assertEquals(71, MapColumns.plane(tiles, 1, 1, 0));
        assertTrue(MapColumns.trunk(tile, MapTile.index(2, 2)));
        assertEquals(66, MapColumns.solidTop(tile, MapTile.index(2, 2)));
        assertFalse(MapColumns.floats(tile, MapTile.index(3, 3)));
        assertEquals(66, MapColumns.solidTop(tile, MapTile.index(3, 3)));
        assertEquals(MapTerrain.FOLIAGE, MapColumns.base(tile, MapTile.index(3, 3)));
    }

    @Test
    @DisplayName("a canopy over water floats over water, the base reads as water")
    void canopyOverWater() {
        MapTestTiles tiles = new MapTestTiles().fill(0, 0, 0, 0, (x, z) -> 62, (x, z) -> MapTerrain.WATER)
                .tree(5, 5, 66, 68, false);
        MapTile tile = tiles.tile(0, 0);
        assertEquals(MapTerrain.WATER, MapColumns.base(tile, MapTile.index(5, 5)));
        assertEquals(MapTerrain.FOLIAGE, MapColumns.terrain(tile, MapTile.index(5, 5)));
    }
}
