package net.alex.guzhenren.gameplay.immortalaperture.farm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldPlotTest {

    @Test
    @DisplayName("the layer is the ground most columns share, not the median")
    void commonest() {
        assertEquals(64, FieldPlot.layer(new int[] {64, 64, 64, 64, 65, 65, 66, 66, 66}),
                "four columns at 64 beat a median of 65");
        assertEquals(67, FieldPlot.layer(new int[] {67, 64, 67, 65, 67}), "the grounds need not be sorted");
    }

    @Test
    @DisplayName("a tie goes to the layer nearer the median, then to the lower one")
    void tie() {
        assertEquals(65, FieldPlot.layer(new int[] {64, 64, 65, 65, 66, 66}), "the median 65 is among the tied");
        assertEquals(64, FieldPlot.layer(new int[] {64, 64, 65, 66, 66}), "64 and 66 are both one off the median");
        assertEquals(64, FieldPlot.layer(new int[] {64, 64, 65, 67, 67}), "65 is the median, 64 is nearer than 67");
    }
}
