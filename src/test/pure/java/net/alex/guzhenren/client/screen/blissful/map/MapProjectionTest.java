package net.alex.guzhenren.client.screen.blissful.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapProjectionTest {

    private static final double EPS = 1.0E-9;

    @Test
    @DisplayName("zoom levels give 4, 8 and 16 map pixel wide diamonds")
    void zoomLevels() {
        assertEquals(4, MapProjection.of(0, 0, false).blockWidth());
        assertEquals(8, MapProjection.of(1, 0, false).blockWidth());
        assertEquals(16, MapProjection.of(2, 0, false).blockWidth());
        assertEquals(16, MapProjection.of(9, 0, false).blockWidth());
        assertEquals(3, MapProjection.of(0, -1, false).rotation());
    }

    @Test
    @DisplayName("column and continuous rotations invert for all four turns")
    void rotationsInvert() {
        for (int rotation = 0; rotation < 4; rotation++) {
            MapProjection projection = MapProjection.of(1, rotation, false);
            for (int x = -20; x <= 20; x += 3) {
                for (int z = -20; z <= 20; z += 5) {
                    int u = projection.columnU(x, z);
                    int v = projection.columnV(x, z);
                    assertEquals(x, projection.columnX(u, v));
                    assertEquals(z, projection.columnZ(u, v));
                    assertEquals(u, (int) Math.floor(projection.u(x + 0.5, z + 0.5)));
                    assertEquals(v, (int) Math.floor(projection.v(x + 0.5, z + 0.5)));
                }
            }
        }
    }

    @Test
    @DisplayName("turning once moves east to where south was: the map turns clockwise")
    void rotationIsClockwise() {
        MapProjection straight = MapProjection.of(1, 0, false);
        MapProjection turned = MapProjection.of(1, 1, false);
        assertEquals(straight.screenX(0, 0, 1), turned.screenX(1, 0, 0), EPS);
        assertEquals(straight.screenY(0, 0, 1), turned.screenY(1, 0, 0), EPS);
    }

    @Test
    @DisplayName("world to screen and back lands on the same point in both views")
    void projectionInverts() {
        for (boolean plan : new boolean[] { false, true }) {
            for (int rotation = 0; rotation < 4; rotation++) {
                MapProjection projection = MapProjection.of(2, rotation, plan);
                double x = 12.25;
                double y = 70.0;
                double z = -33.5;
                double screenX = projection.screenX(x, y, z);
                double screenY = projection.screenY(x, y, z);
                assertEquals(x, projection.worldX(screenX, screenY, y), EPS);
                assertEquals(z, projection.worldZ(screenX, screenY, y), EPS);
            }
        }
    }

    @Test
    @DisplayName("on flat ground every pixel belongs to exactly one top face")
    void topFacesTile() {
        for (int zoom = 0; zoom < MapProjection.ZOOM_LEVELS; zoom++) {
            MapProjection projection = MapProjection.of(zoom, 0, false);
            int[][] owners = new int[40][40];
            for (int u = -30; u <= 30; u++) {
                for (int v = -30; v <= 30; v++) {
                    int anchorX = projection.anchorX(u, v);
                    int anchorY = projection.anchorY(u, v, 0);
                    for (int py = 0; py < 40; py++) {
                        for (int px = 0; px < 40; px++) {
                            if (projection.face(px - anchorX, py - anchorY, 0, 0) == MapProjection.Face.TOP) {
                                owners[py][px]++;
                            }
                        }
                    }
                }
            }
            for (int[] row : owners) {
                for (int count : row) assertEquals(1, count, "zoom " + zoom);
            }
        }
    }

    @Test
    @DisplayName("a raised column's side faces start right under its top face and run 2q rows per exposed block")
    void sideFacesHangUnderTheTop() {
        MapProjection projection = MapProjection.of(1, 0, false);
        int cell = projection.cell();
        for (int column = -cell; column < cell; column++) {
            int lastTop = -1;
            for (int row = 0; row < cell; row++) {
                if (projection.face(column, row, 0, 0) == MapProjection.Face.TOP) lastTop = row;
            }
            int start = column < 0 ? projection.leftStart(column) : projection.rightStart(column);
            if (lastTop >= 0) assertEquals(lastTop + 1, start, "column " + column);
            MapProjection.Face side = column < 0 ? MapProjection.Face.LEFT : MapProjection.Face.RIGHT;
            assertEquals(side, projection.face(column, start + 2 * cell - 1, 2, 2));
            assertNull(projection.face(column, start + 2 * cell, 2, 2));
        }
    }
}
