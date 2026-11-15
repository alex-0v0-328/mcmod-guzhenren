package net.alex.guzhenren.client;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteBatchTest {

    @Test
    @DisplayName("a sprite drawn at its own size is one region")
    void nativeSizeIsOneRegion() {
        List<int[]> regions = slice(76, 20, 76, 20, 4, 4, 4, 4);
        assertEquals(1, regions.size());
        assertEquals(List.of(0, 0, 76, 20, 0, 0), toList(regions.getFirst()));
    }

    @Test
    @DisplayName("the right column keeps the right border's width when the side borders differ")
    void rightColumnUsesTheRightBorder() {
        List<int[]> regions = slice(102, 52, 34, 34, 6, 6, 8, 8);
        assertCoveredOnce(regions, 102, 52);
        for (int[] r : regions) {
            if (r[0] + r[2] <= 94) continue;
            assertEquals(94, r[0], "a right-column region starts 8 pixels from the edge");
            assertEquals(8, r[2]);
            assertEquals(26, r[4], "it reads the sprite's last 8 columns");
        }
    }

    @Test
    @DisplayName("the center tiles at the sprite's center size and a partial tile is cropped, not scaled")
    void centreTilesAndCrops() {
        List<int[]> regions = slice(50, 32, 32, 32, 6, 6, 6, 6);
        assertCoveredOnce(regions, 50, 32);
        List<Integer> centreWidths = new ArrayList<>();
        for (int[] r : regions) {
            if (r[1] == 6 && r[0] >= 6 && r[0] < 44) centreWidths.add(r[2]);
        }
        assertEquals(List.of(20, 18), centreWidths);
    }

    @Test
    @DisplayName("a zero border emits no empty region")
    void zeroBorderEmitsNothingEmpty() {
        List<int[]> regions = slice(50, 4, 32, 4, 0, 1, 0, 1);
        assertCoveredOnce(regions, 50, 4);
        for (int[] r : regions) assertTrue(r[2] > 0 && r[3] > 0);
    }

    private static List<int[]> slice(int width, int height, int texW, int texH, int left, int top, int right,
                                     int bottom) {
        List<int[]> regions = new ArrayList<>();
        SpriteBatch.slice(width, height, texW, texH, left, top, right, bottom,
                (x, y, w, h, u, v) -> regions.add(new int[] { x, y, w, h, u, v }));
        return regions;
    }

    private static void assertCoveredOnce(List<int[]> regions, int width, int height) {
        int[] hits = new int[width * height];
        for (int[] r : regions) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) hits[y * width + x]++;
            }
        }
        for (int i = 0; i < hits.length; i++) {
            assertEquals(1, hits[i], "pixel " + (i % width) + "," + (i / width) + " is drawn once");
        }
    }

    private static List<Integer> toList(int[] region) {
        List<Integer> out = new ArrayList<>();
        for (int value : region) out.add(value);
        return out;
    }
}
