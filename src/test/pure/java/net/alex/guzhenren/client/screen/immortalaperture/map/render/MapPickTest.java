package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import java.util.HashSet;
import java.util.Set;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPickTest {

    private static final MapTerrain[] LAND = { MapTerrain.GRASS, MapTerrain.STONE, MapTerrain.SAND,
            MapTerrain.FOLIAGE, MapTerrain.SOULSUNDER_STONE, MapTerrain.WATER, MapTerrain.FENCE };

    private static MapTestTiles rugged() {
        MapTestTiles tiles = new MapTestTiles().fill(-4, -4, 3, 3,
                (x, z) -> 64 + Math.floorMod(MapTestTiles.hash(x, z), 9),
                (x, z) -> LAND[Math.floorMod(MapTestTiles.hash(z, x), LAND.length)]);
        for (int x = -30; x < 30; x += 5) {
            for (int z = -30; z < 30; z += 4) {
                int ground = tiles.tile(x >> 4, z >> 4).groundAt(x, z);
                MapTerrain here = tiles.tile(x >> 4, z >> 4).terrainAt(x, z);
                if (here == MapTerrain.FOLIAGE || here == MapTerrain.FENCE) continue;
                int hash = Math.floorMod(MapTestTiles.hash(x * 3, z), 3);
                tiles.tree(x, z, ground + 2 + hash, ground + 4 + hash, hash == 0);
            }
        }
        for (int x = -29; x < 30; x += 6) {
            for (int z = -29; z < 30; z += 5) {
                int ground = tiles.tile(x >> 4, z >> 4).groundAt(x, z);
                int lift = Math.floorMod(MapTestTiles.hash(z, x * 7), 3);
                tiles.floating(x, z, ground + 9 + lift, ground + 10 + 2 * lift, MapTerrain.STONE);
            }
        }
        return tiles;
    }

    @Test
    @DisplayName("every isometric pixel picks the column and face whose colours were painted there, in all turns and zooms")
    void pickMatchesRasterIso() {
        MapTestTiles tiles = rugged();
        MapGrid grid = tiles.grid(-4, -4, 3, 3);
        for (int zoom = 0; zoom < MapProjection.ZOOM_LEVELS; zoom++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                MapProjection projection = MapProjection.of(zoom, rotation, false);
                int width = 24 * projection.blockWidth();
                int height = 14 * projection.blockWidth();
                int originX = (int) projection.screenX(0, 68, 0) - width / 2;
                int originY = (int) projection.screenY(0, 68, 0) - height / 2;
                MapRaster.Result result = MapRaster.draw(projection, grid, 69, originX, originY, width, height);
                for (int py = 0; py < height; py++) {
                    for (int px = 0; px < width; px++) {
                        MapPick.Hit hit = MapPick.pick(projection, grid, 69, originX + px, originY + py);
                        assertNotNull(hit);
                        assertTrue(hit.known());
                        assertTrue(Math.abs(hit.x()) < 40 && Math.abs(hit.z()) < 40);
                        assertTrue(paints(hit).contains(result.pixels()[py * width + px]),
                                "zoom " + zoom + " rotation " + rotation + " pixel " + px + "," + py);
                    }
                }
            }
        }
    }

    private static Set<Integer> paints(MapPick.Hit hit) {
        MapTerrain terrain = hit.terrain();
        Set<Integer> colours = new HashSet<>();
        if (hit.face() == MapProjection.Face.TOP) {
            colours.add(MapPalette.top(terrain, hit.x(), hit.z()));
            colours.add(MapPalette.rim(terrain));
            if (terrain == MapTerrain.WATER) {
                for (int shore = 1; shore <= MapPalette.SHORE_STEPS; shore++) colours.add(MapPalette.water(shore));
                colours.add(MapPalette.GLINT);
            }
        } else {
            colours.add(MapPalette.colour(terrain, hit.face()));
            colours.add(MapPalette.strata(terrain, hit.face()));
        }
        return colours;
    }

    @Test
    @DisplayName("the centre of a column's top face picks that column back, in both views")
    void topCentreRoundTrips() {
        MapTestTiles tiles = new MapTestTiles().fill(-2, -2, 1, 1, (x, z) -> 70, (x, z) -> MapTerrain.GRASS);
        for (boolean plan : new boolean[] { false, true }) {
            for (int rotation = 0; rotation < 4; rotation++) {
                MapProjection projection = MapProjection.of(1, rotation, plan);
                for (int x = -20; x < 20; x += 3) {
                    for (int z = -20; z < 20; z += 7) {
                        int pixelX = (int) Math.floor(projection.screenX(x + 0.5, 71, z + 0.5));
                        int pixelY = (int) Math.floor(projection.screenY(x + 0.5, 71, z + 0.5));
                        MapPick.Hit hit = MapPick.pick(projection, tiles, 71, pixelX, pixelY);
                        assertNotNull(hit);
                        assertEquals(x, hit.x());
                        assertEquals(z, hit.z());
                        assertEquals(70, hit.ground());
                        assertEquals(MapProjection.Face.TOP, hit.face());
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("a cliff face picks the taller column, a leafy column reports ground and top apart")
    void cliffsAndTrees() {
        MapTestTiles tiles = new MapTestTiles().fill(-1, -1, 0, 0, (x, z) -> x == 0 && z == 0 ? 80 : 64,
                (x, z) -> x == 0 && z == 0 ? MapTerrain.FOLIAGE : MapTerrain.GRASS);
        MapProjection projection = MapProjection.of(1, 0, false);
        int anchorX = projection.anchorX(0, 0);
        int anchorY = projection.anchorY(0, 0, 84);
        MapPick.Hit left = MapPick.pick(projection, tiles, 65, anchorX - 1, anchorY + projection.cell() * 5);
        assertNotNull(left);
        assertEquals(MapProjection.Face.LEFT, left.face());
        assertEquals(0, left.x());
        assertEquals(80, left.ground());
        assertEquals(83, left.top());
        MapPick.Hit right = MapPick.pick(projection, tiles, 65, anchorX + 1, anchorY + projection.cell() * 5);
        assertNotNull(right);
        assertEquals(MapProjection.Face.RIGHT, right.face());
    }

    @Test
    @DisplayName("a floating canopy picks the canopy, the trunk below it and the ground under the gap apart")
    void floatingCanopy() {
        MapTestTiles tiles = new MapTestTiles().fill(-1, -1, 0, 0, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .tree(0, 0, 70, 72, true).tree(3, 0, 68, 69, false);
        MapProjection projection = MapProjection.of(2, 0, false);
        int cell = projection.cell();
        int anchorX = projection.anchorX(0, 0);
        MapPick.Hit canopy = MapPick.pick(projection, tiles, 65, anchorX, projection.anchorY(0, 0, 73) + 1);
        assertNotNull(canopy);
        assertEquals(MapTerrain.FOLIAGE, canopy.terrain());
        assertEquals(72, canopy.top());
        assertEquals(64, canopy.ground());
        MapPick.Hit trunk = MapPick.pick(projection, tiles, 65, anchorX - 1, projection.anchorY(0, 0, 70) + cell * 3);
        assertNotNull(trunk);
        assertEquals(MapTerrain.WOOD, trunk.terrain());
        assertEquals(69, trunk.top());
        int u = projection.columnU(3, 0);
        int v = projection.columnV(3, 0);
        MapPick.Hit under = MapPick.pick(projection, tiles, 65, projection.anchorX(u, v),
                projection.anchorY(u, v, 65) + 1);
        assertNotNull(under);
        assertEquals(3, under.x());
        assertEquals(MapTerrain.GRASS, under.terrain());
        assertEquals(64, under.top());
    }

    @Test
    @DisplayName("a pixel in the air under a floating block picks the grass behind it, not the block's column")
    void gapUnderFloatingBlock() {
        MapTestTiles tiles = new MapTestTiles().fill(-2, -2, 1, 1, (x, z) -> 64, (x, z) -> MapTerrain.GRASS)
                .floating(0, 0, 80, 80, MapTerrain.STONE);
        MapProjection projection = MapProjection.of(1, 0, false);
        int cell = projection.cell();
        int anchorX = projection.anchorX(0, 0);
        int stoneY = projection.anchorY(0, 0, 81);
        MapPick.Hit stone = MapPick.pick(projection, tiles, 65, anchorX - 1, stoneY + cell + 1);
        assertNotNull(stone);
        assertEquals(MapTerrain.STONE, stone.terrain());
        assertEquals(MapProjection.Face.LEFT, stone.face());
        MapPick.Hit gap = MapPick.pick(projection, tiles, 65, anchorX - 1, stoneY + cell * 8);
        assertNotNull(gap);
        assertEquals(MapTerrain.GRASS, gap.terrain());
        assertEquals(64, gap.top());
        assertEquals(MapProjection.Face.TOP, gap.face());
        assertFalse(gap.x() == 0 && gap.z() == 0);
    }

    @Test
    @DisplayName("fog picks report unknown, void and empty space pick nothing")
    void fogAndVoid() {
        MapTestTiles tiles = new MapTestTiles().put(MapTile.empty(0, 0));
        MapProjection projection = MapProjection.of(1, 0, false);
        MapPick.Hit fog = MapPick.pick(projection, tiles, 64, (int) projection.screenX(-8.5, 64, -8.5),
                (int) projection.screenY(-8.5, 64, -8.5));
        assertNotNull(fog);
        assertFalse(fog.known());
        assertEquals(MapTerrain.UNKNOWN, fog.terrain());
        assertNull(MapPick.pick(projection, tiles, 64, (int) projection.screenX(12.5, 64, 12.5),
                (int) projection.screenY(12.5, 64, 12.5)));
    }
}
