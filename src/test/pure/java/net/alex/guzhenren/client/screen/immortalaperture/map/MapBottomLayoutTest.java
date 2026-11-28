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
        MapTools tools = new MapTools();
        MapFieldBar bar = new MapFieldBar(null);
        MapLegend legend = new MapLegend();
        tools.layout(width, height);
        bar.layout(width, height, tools.top());
        legend.layout(width, height, Math.max(tools.boxWidth(), MapObjectBar.WIDTH), bar.hintTop());
        int[] toolBox = tools.box();
        int[] barBox = bar.box();
        int[] legendBox = legend.box();
        for (int[] box : new int[][] {toolBox, barBox, legendBox}) {
            assertTrue(inside(box, width, height), "every bottom widget stays on screen");
        }
        assertFalse(overlap(barBox, toolBox), "the object bar sits above the function bar");
        assertFalse(overlap(barBox, legendBox), "the object bar clears the legend");
        assertFalse(overlap(hint(barBox, bar.hintTop()), legendBox), "the object bar hint clears the legend");
        assertFalse(overlap(toolBox, legendBox), "the function bar clears the legend");
        assertFalse(overlap(hint(toolBox, tools.boxTop()), legendBox), "the browse hint clears the legend");
    }
}
