package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapViewTest {

    private static final double EPS = 1.0E-6;

    @Test
    @DisplayName("day, dusk, night and dawn key tints with linear blends between")
    void tintKeyframes() {
        assertTint(MapTint.DAY, MapTint.at(6000));
        assertTint(MapTint.DUSK, MapTint.at(12500));
        assertTint(MapTint.NIGHT, MapTint.at(18000));
        assertTint(MapTint.DAWN, MapTint.at(23250));
        assertTint(MapTint.DAY, MapTint.at(24000L * 5 + 1000));
        MapTint halfDusk = MapTint.at(11750);
        assertEquals(0.85F, halfDusk.green(), 1.0E-5F);
        assertEquals(0.74F, halfDusk.blue(), 1.0E-5F);
        MapTint morning = MapTint.at(125);
        assertEquals(0.89F, morning.green(), 1.0E-5F);
        assertEquals(0x805C70BD, MapTint.NIGHT.apply(0x80FFFFFF));
    }

    private static void assertTint(MapTint expected, MapTint actual) {
        assertEquals(expected.red(), actual.red(), 1.0E-5F);
        assertEquals(expected.green(), actual.green(), 1.0E-5F);
        assertEquals(expected.blue(), actual.blue(), 1.0E-5F);
    }

    @Test
    @DisplayName("a plan viewport maps to the chunks it covers, rotated views swap the axes")
    void viewportChunks() {
        MapCamera camera = new MapCamera(0, 64, 0);
        camera.setZoom(2);
        camera.setPlan(true);
        camera.setViewport(160, 96);
        assertEquals(new MapView(-1, -1, 0, 0), camera.viewChunks());
        camera.setCenter(40, 64, 0);
        assertEquals(new MapView(1, -1, 3, 0), camera.viewChunks());
        camera.setViewport(320, 96);
        camera.setCenter(0, 64, 0);
        assertEquals(new MapView(-2, -1, 1, 0), camera.viewChunks());
        camera.rotate(1);
        assertEquals(new MapView(-1, -2, 0, 1), camera.viewChunks());
    }

    @Test
    @DisplayName("an oversized isometric viewport is clipped to 64 chunks around its centre")
    void viewportClipsTo64() {
        MapCamera camera = new MapCamera(1000, 64, -500);
        camera.setZoom(0);
        camera.setViewport(2400, 1400);
        MapView view = camera.viewChunks();
        assertTrue(view.isValid());
        assertEquals(MapView.MAX_SIDE, view.width());
        assertEquals(MapView.MAX_SIDE, view.depth());
        assertTrue(view.contains(1000 >> 4, -500 >> 4));
    }

    @Test
    @DisplayName("zooming keeps the point under the cursor, dragging moves the map with the cursor")
    void zoomAndPan() {
        for (boolean plan : new boolean[] { false, true }) {
            MapCamera camera = new MapCamera(100.5, 70, -40.25);
            camera.setPlan(plan);
            camera.rotate(3);
            camera.setViewport(400, 300);
            double pointX = camera.worldX(300, 80);
            double pointZ = camera.worldZ(300, 80);
            assertTrue(camera.zoom(1, 300, 80));
            assertEquals(pointX, camera.worldX(300, 80), 1.0);
            assertEquals(pointZ, camera.worldZ(300, 80), 1.0);
            double beforeX = camera.viewX(90, 70, -30);
            double beforeY = camera.viewY(90, 70, -30);
            camera.pan(25, -10);
            assertEquals(beforeX + 25, camera.viewX(90, 70, -30), 1.0);
            assertEquals(beforeY - 10, camera.viewY(90, 70, -30), 1.0);
            assertEquals(70, camera.y(), EPS);
        }
    }

    @Test
    @DisplayName("redraw on first frame, layout change, leaving the inner frame, or new tiles after 250 ms")
    void redrawTriggers() {
        MapCamera camera = new MapCamera(0, 64, 0);
        camera.setViewport(200, 100);
        MapRedraw shown = MapRedraw.around(camera, 5);
        assertEquals(300, shown.width());
        assertEquals(150, shown.height());
        int viewX = camera.originX();
        int viewY = camera.originY();
        assertEquals(MapRedraw.Reason.FIRST, MapRedraw.check(null, shown, viewX, viewY, 200, 100, 0));
        assertEquals(MapRedraw.Reason.NONE, MapRedraw.check(shown, shown, viewX, viewY, 200, 100, 0));
        assertEquals(MapRedraw.Reason.NONE, MapRedraw.check(shown, shown, viewX + 16, viewY - 8, 200, 100, 0));
        assertEquals(MapRedraw.Reason.PAN, MapRedraw.check(shown, shown, viewX + 40, viewY, 200, 100, 0));
        assertEquals(MapRedraw.Reason.PAN, MapRedraw.check(shown, shown, viewX, viewY - 20, 200, 100, 0));
        MapRedraw newer = MapRedraw.around(camera, 6);
        assertEquals(MapRedraw.Reason.NONE, MapRedraw.check(shown, newer, viewX, viewY, 200, 100, 249));
        assertEquals(MapRedraw.Reason.DATA, MapRedraw.check(shown, newer, viewX, viewY, 200, 100, 250));
        camera.rotate(1);
        assertEquals(MapRedraw.Reason.LAYOUT,
                MapRedraw.check(shown, MapRedraw.around(camera, 5), viewX, viewY, 200, 100, 0));
        camera.rotate(-1);
        camera.setViewport(220, 100);
        assertEquals(MapRedraw.Reason.LAYOUT,
                MapRedraw.check(shown, MapRedraw.around(camera, 5), viewX, viewY, 220, 100, 0));
    }
}
