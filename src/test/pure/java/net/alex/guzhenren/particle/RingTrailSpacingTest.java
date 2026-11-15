package net.alex.guzhenren.particle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** L1：定点冲刺尾迹背后的等距累加器。 */

class RingTrailSpacingTest {

    private static final double EPS = 1.0E-9;

    @Test
    void belowThresholdOnlyCarries() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(0.0D, 1.9D, 7);
        assertEquals(0, drops.offsets().length);
        assertEquals(1.9D, drops.carry(), EPS);
        assertEquals(7, drops.ringsLeft());
    }

    @Test
    void crossingDropsAtTheThreshold() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(0.0D, 2.0D, 7);
        assertArrayEquals(new double[] { 2.0D }, drops.offsets(), EPS);
        assertEquals(0.0D, drops.carry(), EPS);
        assertEquals(6, drops.ringsLeft());
    }

    @Test
    void carryRollsAcrossTicks() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(1.9D, 0.2D, 7);
        assertArrayEquals(new double[] { 0.1D }, drops.offsets(), EPS);
        assertEquals(0.1D, drops.carry(), EPS);
        assertEquals(6, drops.ringsLeft());
    }

    @Test
    void aJumpSpreadsEvenlyAlongTheSegment() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(0.0D, 6.5D, 7);
        assertArrayEquals(new double[] { 2.0D, 4.0D, 6.0D }, drops.offsets(), EPS);
        assertEquals(0.5D, drops.carry(), EPS);
        assertEquals(4, drops.ringsLeft());
    }

    @Test
    void theRingBudgetCapsTheDrops() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(0.0D, 100.0D, 2);
        assertArrayEquals(new double[] { 2.0D, 4.0D }, drops.offsets(), EPS);
        assertEquals(0, drops.ringsLeft());
    }

    @Test
    void aWiderSpacingDropsAtItsOwnStride() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(4.0D, 2.5D, 6.5D, Integer.MAX_VALUE);
        assertArrayEquals(new double[] { 1.5D, 5.5D }, drops.offsets(), EPS);
        assertEquals(1.0D, drops.carry(), EPS);
    }
}
