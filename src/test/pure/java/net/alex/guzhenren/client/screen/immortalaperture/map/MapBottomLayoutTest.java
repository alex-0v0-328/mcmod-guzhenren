package net.alex.guzhenren.client.screen.immortalaperture.map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapBottomLayoutTest {

    private static final int HINT_W = 200;

    private static boolean overlap(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    private static boolean inside(int[] box, int width, int height) {
        return box[0] >= 0 && box[1] >= 0 && box[0] + box[2] <= width && box[1] + box[3] <= height;
    }

    private static int[] hint(int[] bar, int top) {
        return new int[] {bar[0] + bar[2] / 2 - HINT_W / 2, top, HINT_W, MapHud.CHIP_H};
    }

    @ParameterizedTest(name = "{0}×{1}")
    @CsvSource({"427, 267", "480, 270", "640, 400", "960, 540"})
    void bottomWidgetsNeverOverlap(int width, int height) {
        MapActionBar dock = new MapActionBar();
        MapTools tools = new MapTools();
        MapLegend legend = new MapLegend();
        dock.layout(width, height);
        tools.layout(width, height);
        legend.layout(width, height, dock.boxWidth(), dock.hintTop());
        int[] dockBox = dock.box();
        int[] toolBox = tools.box();
        int[] legendBox = legend.box();
        int[] card = MapSelectionCard.pastureBox(MapSelectionCard.bottom(tools, height));
        for (int[] box : new int[][] {dockBox, toolBox, legendBox, card}) {
            assertTrue(inside(box, width, height), "every bottom widget stays on screen");
        }
        assertFalse(overlap(dockBox, legendBox), "the dock clears the legend");
        assertFalse(overlap(hint(dockBox, dock.hintTop()), legendBox), "the dock hint clears the legend");
        assertFalse(overlap(toolBox, legendBox), "the tool bar clears the legend");
        assertFalse(overlap(toolBox, card), "the tool bar clears the pasture card");
        assertFalse(overlap(hint(toolBox, tools.boxTop()), card), "the tool hint clears the pasture card");
    }
}
