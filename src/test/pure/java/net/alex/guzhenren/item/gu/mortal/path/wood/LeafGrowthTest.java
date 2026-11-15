package net.alex.guzhenren.item.gu.mortal.path.wood;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 九叶生机草叶片时钟：按时间戳惰性再生；满株计时器停住；摘叶、长叶保留进度；三种模型外观。 */

class LeafGrowthTest {

    private static final int PERIOD = 3_600;
    private static final long T0 = 100_000L;

    @Test
    @DisplayName("a grass without the component is full, and settling parks its timer at the read")
    void fullParksTheTimer() {
        assertEquals(new LeafGrowth(LeafGrowth.MAX_LEAVES, T0), LeafGrowth.FULL.settle(T0, PERIOD));
    }

    @Test
    @DisplayName("the first pick from a full grass starts the regrowth at the pick")
    void pickFromFullStartsTheWait() {
        LeafGrowth picked = new LeafGrowth(LeafGrowth.MAX_LEAVES, 0L).pick(T0, PERIOD);
        assertEquals(new LeafGrowth(8, T0), picked);
        assertEquals(8, picked.settle(T0 + PERIOD - 1, PERIOD).leaves());
        assertEquals(LeafGrowth.MAX_LEAVES, picked.settle(T0 + PERIOD, PERIOD).leaves());
    }

    @Test
    @DisplayName("one leaf per period, and the time toward the next one is kept")
    void regrowsOnePerPeriodKeepingProgress() {
        LeafGrowth settled = new LeafGrowth(5, T0).settle(T0 + 2L * PERIOD + 7, PERIOD);
        assertEquals(new LeafGrowth(7, T0 + 2L * PERIOD), settled);
        assertEquals(PERIOD - 7, new LeafGrowth(5, T0).getTicksToNextLeaf(T0 + 2L * PERIOD + 7, PERIOD));
    }

    @Test
    @DisplayName("an empty grass is full again after nine periods, and never past nine")
    void capsAtNine() {
        assertEquals(8, new LeafGrowth(0, T0).settle(T0 + 9L * PERIOD - 1, PERIOD).leaves());
        assertEquals(new LeafGrowth(LeafGrowth.MAX_LEAVES, T0 + 9L * PERIOD),
                new LeafGrowth(0, T0).settle(T0 + 9L * PERIOD, PERIOD));
        assertEquals(LeafGrowth.MAX_LEAVES, new LeafGrowth(0, T0).settle(T0 + 100L * PERIOD, PERIOD).leaves());
        assertEquals(0, new LeafGrowth(0, T0).getTicksToNextLeaf(T0 + 100L * PERIOD, PERIOD));
    }

    @Test
    @DisplayName("a grown leaf keeps the partial regrowth, and the ninth parks the timer")
    void growKeepsProgressUntilFull() {
        assertEquals(new LeafGrowth(6, T0), new LeafGrowth(5, T0).grow(T0 + 10, PERIOD));
        assertEquals(new LeafGrowth(LeafGrowth.MAX_LEAVES, T0 + 10), new LeafGrowth(8, T0).grow(T0 + 10, PERIOD));
    }

    @Test
    @DisplayName("picking a bare grass changes nothing")
    void pickAtZeroStaysZero() {
        assertEquals(new LeafGrowth(0, T0), new LeafGrowth(0, T0).pick(T0 + 10, PERIOD));
    }

    @Test
    @DisplayName("a stamp ahead of the clock restarts the wait instead of freezing it")
    void stampAheadOfTheClockRestarts() {
        LeafGrowth settled = new LeafGrowth(3, T0).settle(T0 - 50, PERIOD);
        assertEquals(new LeafGrowth(3, T0 - 50), settled);
        assertEquals(4, settled.settle(T0 - 50 + PERIOD, PERIOD).leaves());
    }

    @Test
    @DisplayName("full looks from seven leaves, half from one, empty at none")
    void modelLooks() {
        assertEquals(0.0F, LeafGrowth.getPickedStage(9));
        assertEquals(0.0F, LeafGrowth.getPickedStage(7));
        assertEquals(LeafGrowth.PICKED_HALF, LeafGrowth.getPickedStage(6));
        assertEquals(LeafGrowth.PICKED_HALF, LeafGrowth.getPickedStage(1));
        assertEquals(LeafGrowth.PICKED_BARE, LeafGrowth.getPickedStage(0));
    }

    @Test
    @DisplayName("the count clamps into 0 to 9")
    void clampsTheCount() {
        assertEquals(LeafGrowth.MAX_LEAVES, new LeafGrowth(12, T0).leaves());
        assertEquals(0, new LeafGrowth(-1, T0).leaves());
    }

    @Test
    @DisplayName("a grass that fills between two periods is stamped at the read, however far past the ninth leaf")
    void aGrassThatFillsIsStampedAtTheRead() {
        assertEquals(new LeafGrowth(LeafGrowth.MAX_LEAVES, T0 + 9L * PERIOD + 5),
                new LeafGrowth(0, T0).settle(T0 + 9L * PERIOD + 5, PERIOD));
        assertEquals(new LeafGrowth(LeafGrowth.MAX_LEAVES, T0 + 100L * PERIOD + 3),
                new LeafGrowth(0, T0).settle(T0 + 100L * PERIOD + 3, PERIOD));
    }
}
