package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import java.util.Arrays;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapColumnsTest {

    private static int[] exposed(TileSource tiles, int fogPlane, int x, int z, int top, int bottom) {
        int[] out = new int[MapColumns.EXPOSED_LENGTH];
        int count = MapColumns.exposed(tiles, fogPlane, x, z, top, bottom, out);
        return Arrays.copyOf(out, 2 * count);
    }

    @Test
    @DisplayName("a floating block shows only its own block of face, the air under it hides no cliff")
    void floatingBlockExposesOnlyItself() {
        MapTestTiles tiles = new MapTestTiles().fill(-1, -1, 0, 0, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .floating(0, 0, 80, 80, MapTerrain.STONE);
        assertArrayEquals(new int[] { 0, 1 }, exposed(tiles, 0, 0, 1, 80, 80));
        assertArrayEquals(new int[] { 0, 1 }, exposed(tiles, 0, 1, 0, 80, 80));
        assertArrayEquals(new int[0], exposed(tiles, 0, 0, 1, 64, MapTile.OPEN));
        assertArrayEquals(new int[0], exposed(tiles, 0, 0, 0, 64, MapTile.OPEN));
        MapTile tile = tiles.tile(0, 0);
        assertEquals(2, tile.runs(MapTile.index(0, 0)));
        assertEquals(80, MapColumns.top(tile, MapTile.index(0, 0)));
        assertEquals(MapTerrain.STONE, MapColumns.terrain(tile, MapTile.index(0, 0)));
    }

    @Test
    @DisplayName("a cliff face breaks around a block floating in front of it")
    void cliffBreaksAroundFloatingBlock() {
        MapTestTiles tiles = new MapTestTiles().fill(-1, -1, 0, 0, (x, z) -> x == 0 && z == 0 ? 80 : 64,
                (x, z) -> MapTerrain.STONE).floating(0, 1, 70, 72, MapTerrain.WOOD);
        assertArrayEquals(new int[] { 0, 8, 11, 16 }, exposed(tiles, 0, 0, 1, 80, MapTile.OPEN));
        assertArrayEquals(new int[] { 0, 16 }, exposed(tiles, 0, 1, 0, 80, MapTile.OPEN));
    }

    @Test
    @DisplayName("ground drops an 8-block skirt toward the void, fog covers everything under the fog plane")
    void voidSkirtAndFog() {
        MapTestTiles tiles = new MapTestTiles().fill(0, 0, 0, 0, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .put(MapTile.empty(1, 0));
        assertArrayEquals(new int[] { 0, MapColumns.VOID_SKIRT }, exposed(tiles, 0, 16, 0, 64, MapTile.OPEN));
        assertArrayEquals(new int[] { 0, 3 }, exposed(tiles, 0, 16, 0, 64, 62));
        assertArrayEquals(new int[] { 0, 5 }, exposed(tiles, 60, 0, 16, 64, MapTile.OPEN));
        assertArrayEquals(new int[0], exposed(tiles, 70, 0, 16, 64, MapTile.OPEN));
        assertTrue(MapColumns.isVoid(tiles.tile(1, 0), 0));
    }

    @Test
    @DisplayName("a canopy over water keeps the water under it wet, a bridge over water is dry")
    void wetness() {
        MapTestTiles tiles = new MapTestTiles().fill(0, 0, 0, 0, (x, z) -> 62, (x, z) -> MapTerrain.WATER)
                .tree(5, 5, 66, 68, false);
        MapTile tile = tiles.tile(0, 0);
        assertTrue(MapColumns.wet(tile, MapTile.index(5, 5)));
        assertEquals(MapTerrain.FOLIAGE, MapColumns.terrain(tile, MapTile.index(5, 5)));
        assertTrue(MapColumns.wet(tile, MapTile.index(1, 1)));
        MapTile bridge = new MapTile.Builder().ground(0, 64).run(0, 64, 64, MapTerrain.WOOD)
                .run(0, 62, MapTile.OPEN, MapTerrain.WATER).build(0, 0);
        assertFalse(MapColumns.wet(bridge, 0));
    }
}
