package net.alex.guzhenren.gameplay.rewind;

import java.util.List;
import java.util.function.ToLongFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 如故蛊[As Before Gu]快照环算术：每间隔到期一次快照；环保留一天；「一天前」取距今至少一天减一间隔的最新时间戳。 */

class RewindClockTest {

    private static final ToLongFunction<Long> STAMP = Long::longValue;

    @Test
    @DisplayName("the cadence spans one day: two snapshots a day, three kept, a day ago is 24,000 or older")
    void constantsSpanOneDay() {
        assertEquals(24_000, RewindClock.DAY_TICKS);
        assertEquals(12_000, RewindClock.INTERVAL_TICKS);
        assertEquals(3, RewindClock.RING_SIZE);
        assertEquals(24_000, RewindClock.MIN_AGE_TICKS);
    }

    @Test
    @DisplayName("a snapshot is due with no stamp, after an interval, or when the clock ran backwards")
    void dueOncePerInterval() {
        assertTrue(RewindClock.isDue(RewindClock.NEVER, 0L));
        assertFalse(RewindClock.isDue(0L, 11_999L));
        assertTrue(RewindClock.isDue(0L, 12_000L));
        assertTrue(RewindClock.isDue(5_000L, 100L));
    }

    @Test
    @DisplayName("a day ago is the youngest stamp at least a day old, and none when every stamp is younger")
    void picksTheYoungestOldEnoughStamp() {
        List<Long> ring = List.of(0L, 12_000L, 24_000L);
        assertEquals(12_000L, RewindClock.pick(ring, STAMP, 36_000L));
        assertEquals(0L, RewindClock.pick(ring, STAMP, 35_999L));
        assertNull(RewindClock.pick(ring, STAMP, 23_999L));
        assertNull(RewindClock.pick(List.of(), STAMP, 100_000L));
    }

    @Test
    @DisplayName("adding keeps the newest three and drops any stamp from the future")
    void addKeepsThreeAndDropsTheFuture() {
        List<Long> ring = List.of();
        for (long stamp = 0L; stamp <= 48_000L; stamp += 12_000L) ring = RewindClock.add(ring, stamp, STAMP);
        assertEquals(RewindClock.RING_SIZE, ring.size());
        assertEquals(24_000L, ring.getFirst());
        assertEquals(48_000L, ring.getLast());
        assertEquals(48_000L, RewindClock.lastTakenAt(ring, STAMP));

        List<Long> rewound = RewindClock.add(ring, 30_000L, STAMP);
        assertEquals(List.of(24_000L, 30_000L), rewound);
        assertEquals(RewindClock.NEVER, RewindClock.lastTakenAt(List.of(), STAMP));
    }
}
