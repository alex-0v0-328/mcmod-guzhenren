package net.alex.guzhenren.client.screen.blissful.map;

import java.util.Arrays;
import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;
import net.alex.guzhenren.gameplay.blissful.map.MapTile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRasterTest {

    @Test
    @DisplayName("contours every 8 blocks, the highest 40 multiple wins when a step crosses several")
    void contourLevels() {
        assertEquals(64, MapContours.level(64, 63));
        assertEquals(64, MapContours.level(63, 64));
        assertEquals(MapContours.NONE, MapContours.level(70, 65));
        assertEquals(MapContours.NONE, MapContours.level(70, 70));
        assertEquals(80, MapContours.level(81, 79));
        assertEquals(80, MapContours.level(90, 70));
        assertEquals(88, MapContours.level(95, 85));
        assertEquals(0, MapContours.level(3, -5));
        assertEquals(-40, MapContours.level(-40, -41));
    }

    @Test
    @DisplayName("the plan view draws a thick line and one label where the ground steps over 80")
    void planDrawsMajorContour() {
        MapTestTiles tiles = new MapTestTiles().fill(-2, -2, 1, 1, (x, z) -> x < 0 ? 78 : 82,
                (x, z) -> MapTerrain.GRASS);
        MapProjection projection = MapProjection.of(1, 0, true);
        int cell = projection.cell();
        MapRaster.Result result = MapRaster.draw(projection, tiles.grid(-2, -2, 1, 1), 80, -10 * cell, -10 * cell,
                20 * cell, 20 * cell);
        int row = 5 * cell;
        int boundary = 10 * cell;
        int lit = MapPalette.top(MapTerrain.GRASS);
        assertEquals(MapPalette.darken(lit, 0.40F), result.pixels()[row * result.width() + boundary]);
        assertEquals(MapPalette.darken(lit, 0.40F), result.pixels()[row * result.width() + boundary + 1]);
        assertEquals(lit, result.pixels()[row * result.width() + boundary + 2]);
        assertEquals(1, result.labels().size());
        assertEquals(80, result.labels().getFirst().level());
    }

    @Test
    @DisplayName("an empty grid is all fog and missing or void chunks never throw")
    void missingTilesDrawFog() {
        MapGrid empty = new MapTestTiles().grid(0, 0, 3, 3);
        for (boolean plan : new boolean[] { false, true }) {
            MapProjection projection = MapProjection.of(0, 2, plan);
            MapRaster.Result result = MapRaster.draw(projection, empty, 64, -200, -150, 400, 300);
            assertTrue(Arrays.stream(result.pixels()).allMatch(pixel -> pixel == MapPalette.FOG
                    || pixel == MapPalette.FOG_ALT || pixel == MapPalette.FOG_SIDE));
        }
        MapTestTiles cededTiles = new MapTestTiles();
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) cededTiles.put(MapTile.empty(chunkX, chunkZ));
        }
        MapGrid ceded = cededTiles.grid(-1, -1, 1, 1);
        MapProjection projection = MapProjection.of(1, 0, false);
        int x = (int) projection.screenX(8, 64, 8);
        int y = (int) projection.screenY(8, 64, 8);
        MapRaster.Result result = MapRaster.draw(projection, ceded, 64, x - 4, y - 4, 8, 8);
        assertTrue(Arrays.stream(result.pixels()).allMatch(pixel -> pixel == 0));
    }

    @Test
    @DisplayName("land fades into fog over six blocks from an unsent chunk")
    void fogFade() {
        int west = MapGrid.maskOf(true, false, false, false);
        assertEquals(0.5F / MapGrid.FADE_BLOCKS, MapGrid.fade(west, 0, 8), 1.0E-6F);
        assertEquals(1.0F, MapGrid.fade(west, 6, 8), 1.0E-6F);
        assertEquals(1.0F, MapGrid.fade(0, 0, 0), 1.0E-6F);
        int both = MapGrid.maskOf(true, false, true, false);
        assertEquals(0.5F / MapGrid.FADE_BLOCKS, MapGrid.fade(both, 7, 0), 1.0E-6F);

        MapGrid grid = new MapTestTiles().fill(0, 0, 0, 0, (x, z) -> 64, (x, z) -> MapTerrain.GRASS).grid(0, 0, 0, 0);
        assertTrue(grid.fade(0, 0, 0, 0) < 0.1F);
        assertEquals(0.0F, grid.fade(5, 5, 0, 0));
    }

    @Test
    @DisplayName("isometric raster paints known columns with their palette colour away from the fog")
    void isoPaintsPalette() {
        MapTestTiles tiles = new MapTestTiles().fill(-3, -3, 2, 2, (x, z) -> 64, (x, z) -> MapTerrain.SOULSUNDER_STONE);
        MapProjection projection = MapProjection.of(2, 1, false);
        int x = (int) projection.screenX(0.5, 65, 0.5);
        int y = (int) projection.screenY(0.5, 65, 0.5);
        MapRaster.Result result = MapRaster.draw(projection, tiles.grid(-3, -3, 2, 2), 65, x - 32, y - 16, 64, 32);
        assertTrue(Arrays.stream(result.pixels()).allMatch(pixel -> pixel == 0xFFF8ACC4));
        assertFalse(result.labels().iterator().hasNext());
    }
}
