package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
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
        int lit = MapPalette.top(MapTerrain.GRASS, projection.columnX(0, -5), projection.columnZ(0, -5));
        assertEquals(MapPalette.darken(lit, MapContours.MAJOR_SHADE), result.pixels()[row * result.width() + boundary]);
        assertEquals(MapPalette.darken(lit, MapContours.MAJOR_SHADE),
                result.pixels()[row * result.width() + boundary + 1]);
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
    @DisplayName("a flat field paints only its top colour and grain away from the fog, with some grain showing")
    void isoPaintsPalette() {
        MapTestTiles tiles = new MapTestTiles().fill(-3, -3, 2, 2, (x, z) -> 64, (x, z) -> MapTerrain.SOULSUNDER_STONE);
        MapProjection projection = MapProjection.of(2, 1, false);
        int x = (int) projection.screenX(0.5, 65, 0.5);
        int y = (int) projection.screenY(0.5, 65, 0.5);
        MapRaster.Result result = MapRaster.draw(projection, tiles.grid(-3, -3, 2, 2), 65, x - 64, y - 32, 128, 64);
        int lit = MapPalette.top(MapTerrain.SOULSUNDER_STONE);
        int grain = MapPalette.darken(lit, MapPalette.GRAIN);
        assertTrue(Arrays.stream(result.pixels()).allMatch(pixel -> pixel == lit || pixel == grain));
        assertTrue(Arrays.stream(result.pixels()).anyMatch(pixel -> pixel == grain));
        assertFalse(result.labels().iterator().hasNext());
    }

    @Test
    @DisplayName("a 10-block grass cliff gets a lit rim, one block of grass side, then earth strata")
    void rimAndStrata() {
        MapTestTiles tiles = new MapTestTiles().fill(-2, -2, 1, 1, (x, z) -> x == 0 && z == 0 ? 80 : 70,
                (x, z) -> MapTerrain.GRASS);
        MapProjection projection = MapProjection.of(1, 0, false);
        int cell = projection.cell();
        int anchorX = projection.anchorX(0, 0);
        int anchorY = projection.anchorY(0, 0, 81);
        int originX = anchorX - 16;
        int originY = anchorY - 8;
        MapRaster.Result result = MapRaster.draw(projection, tiles.grid(-2, -2, 1, 1), 71, originX, originY, 32, 64);
        int width = result.width();
        int sideX = anchorX - 1 - originX;
        assertEquals(MapPalette.rim(MapTerrain.GRASS), result.pixels()[(anchorY + cell - 1 - originY) * width + sideX]);
        assertEquals(MapPalette.left(MapTerrain.GRASS), result.pixels()[(anchorY + cell + 1 - originY) * width + sideX]);
        assertEquals(MapPalette.strata(MapTerrain.GRASS, MapProjection.Face.LEFT),
                result.pixels()[(anchorY + cell * 4 - originY) * width + sideX]);
    }

    @Test
    @DisplayName("a floating stone draws only its own block, the air under it shows the grass behind, not a pillar")
    void floatingBlockLeavesTheAirEmpty() {
        MapTestTiles tiles = new MapTestTiles().fill(-3, -3, 2, 2, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .floating(0, 0, 80, 80, MapTerrain.STONE);
        MapProjection projection = MapProjection.of(1, 0, false);
        int cell = projection.cell();
        int anchorX = projection.anchorX(0, 0);
        int stoneY = projection.anchorY(0, 0, 81);
        int groundY = projection.anchorY(0, 0, 65);
        int originX = anchorX - 16;
        int originY = stoneY - 4;
        MapRaster.Result result = MapRaster.draw(projection, tiles.grid(-3, -3, 2, 2), 65, originX, originY, 32,
                groundY + 8 - originY);
        int width = result.width();
        int grass = MapPalette.top(MapTerrain.GRASS);
        Set<Integer> lawn = Set.of(grass, MapPalette.darken(grass, MapPalette.GRAIN));
        Set<Integer> stoneSides = new HashSet<>(List.of(MapPalette.left(MapTerrain.STONE),
                MapPalette.right(MapTerrain.STONE)));
        assertEquals(MapPalette.left(MapTerrain.STONE),
                result.pixels()[(stoneY + cell + 1 - originY) * width + anchorX - 1 - originX]);
        for (int row = stoneY + 2 * cell - originY; row < result.height(); row++) {
            for (int column = 0; column < width; column++) {
                assertFalse(stoneSides.contains(result.pixels()[row * width + column]),
                        "no stone face below the floating block at " + column + "," + row);
            }
        }
        assertTrue(lawn.contains(result.pixels()[(stoneY + cell * 8 - originY) * width + anchorX - 1 - originX]));
        assertTrue(lawn.contains(result.pixels()[(groundY + 1 - originY) * width + anchorX - originX]));
    }

    @Test
    @DisplayName("water darkens by distance to the nearest dry column, fog is not shore")
    void shoreSteps() {
        MapTestTiles tiles = new MapTestTiles().fill(-2, -2, 1, 1, (x, z) -> 62,
                (x, z) -> x == 0 ? MapTerrain.SAND : MapTerrain.WATER);
        MapGrid grid = tiles.grid(-2, -2, 1, 1);
        assertEquals(1, MapRaster.shoreDistance(grid, 1, 5));
        assertEquals(2, MapRaster.shoreDistance(grid, -2, 5));
        assertEquals(3, MapRaster.shoreDistance(grid, 3, -7));
        assertEquals(MapPalette.SHORE_STEPS, MapRaster.shoreDistance(grid, 9, 0));
        assertEquals(MapPalette.SHORE_STEPS, MapRaster.shoreDistance(grid, 31, 0));
        assertEquals(MapPalette.water(1), MapPalette.water(0));
        assertEquals(MapPalette.water(MapPalette.SHORE_STEPS), MapPalette.water(99));
    }
}
