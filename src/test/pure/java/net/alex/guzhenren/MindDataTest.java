package net.alex.guzhenren;

import java.util.Map;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindPool;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindDataTest {

    private static MindData mind(long current, long evil) {
        return new MindData(Brilliance.ORDINARY,
                Map.of(MindPoolType.THOUGHTS, new MindPool(current, 50_000L, false)),
                evil > 0L ? Map.of(ThoughtTag.EVIL, evil) : Map.of());
    }

    private static long sumTagged(MindData d) {
        return d.taggedThoughts().values().stream().mapToLong(Long::longValue).sum();
    }

    @Test
    void extremeCapDoesNotOverflowBurstThreshold() {
        MindPool pool = new MindPool(Long.MAX_VALUE, Long.MAX_VALUE, false);
        assertEquals(Long.MAX_VALUE, pool.burstAt());
        assertFalse(pool.isOverflowing());
    }

    @Test
    void largeTaggedThoughtsScaleWithoutMultiplicationOverflow() {
        assertEquals(Long.MAX_VALUE / 2, mind(Long.MAX_VALUE / 2, Long.MAX_VALUE)
                .taggedThoughts().get(ThoughtTag.EVIL));
    }

    @Test
    void malformedNegativeTagsAreRemoved() {
        MindData data = new MindData(Brilliance.ORDINARY, Map.of(), Map.of(ThoughtTag.EVIL, -1L));
        assertTrue(data.taggedThoughts().isEmpty());
    }

    @Test
    @DisplayName("tagged thoughts clamp to the pool's current -- the sum never exceeds it")
    void taggedClampedToCurrent() {
        MindData d = mind(30, 50);
        assertEquals(30L, d.taggedThoughts().get(ThoughtTag.EVIL));
        assertEquals(30L, sumTagged(d));
    }

    @Test
    @DisplayName("natural thoughts are derived: current minus the tagged sum")
    void naturalIsDerived() {
        MindData d = mind(100, 30);
        assertEquals(100L, d.getPool(MindPoolType.THOUGHTS).current());
        assertEquals(Map.of(ThoughtTag.EVIL, 30L), d.taggedThoughts());
        assertEquals(30L, sumTagged(d), "so 70 of the 100 are natural");
    }

    @Test
    @DisplayName("an empty mind holds no tagged thoughts -- they clamp to zero and drop out")
    void emptyMindDropsTagged() {
        assertTrue(mind(0, 50).taggedThoughts().isEmpty());
    }

    @Test
    @DisplayName("NATURAL is derived, never stored -- the ctor drops it")
    void naturalNeverStored() {
        MindData d = new MindData(Brilliance.ORDINARY,
                Map.of(MindPoolType.THOUGHTS, new MindPool(100, 50_000L, false)),
                Map.of(ThoughtTag.NATURAL, 999L));
        assertFalse(d.taggedThoughts().containsKey(ThoughtTag.NATURAL));
        assertTrue(d.taggedThoughts().isEmpty());
    }

    @Test
    @DisplayName("withTagged removes a tag when the amount falls to zero")
    void withTaggedRemovesZero() {
        MindData d = mind(100, 30).withTagged(ThoughtTag.EVIL, 0);
        assertFalse(d.taggedThoughts().containsKey(ThoughtTag.EVIL));
    }

    @Test
    @DisplayName("emptied clears the tagged thoughts alongside the pools")
    void emptiedClearsTagged() {
        MindData d = mind(100, 30).withEmptied();
        assertTrue(d.taggedThoughts().isEmpty());
        assertEquals(0L, d.getPool(MindPoolType.THOUGHTS).current());
    }

    @Test
    @DisplayName("tagging a thought keeps the brilliance, the pools and the other tags, and replaces its own amount")
    void withTaggedCarriesTheRest() {
        MindData base = new MindData(Brilliance.OUTSTANDING,
                Map.of(MindPoolType.THOUGHTS, new MindPool(100, 7_000L, true)), Map.of());
        MindData tagged = base.withTagged(ThoughtTag.EVIL, 1L);
        assertEquals(Map.of(ThoughtTag.EVIL, 1L), tagged.taggedThoughts());
        assertEquals(Brilliance.OUTSTANDING, tagged.brilliance());
        assertEquals(base.pools(), tagged.pools());
        assertEquals(Map.of(ThoughtTag.EVIL, 40L), tagged.withTagged(ThoughtTag.EVIL, 40L).taggedThoughts());
        assertTrue(tagged.withTagged(ThoughtTag.EVIL, 0L).taggedThoughts().isEmpty());
    }

    @Test
    @DisplayName("emptying a mind keeps the brilliance and each pool's size, and ends the buffer")
    void emptiedKeepsTheShape() {
        MindData full = new MindData(Brilliance.DECENT,
                Map.of(MindPoolType.THOUGHTS, new MindPool(100, 7_000L, true)), Map.of(ThoughtTag.EVIL, 30L));
        MindData emptied = full.withEmptied();
        assertEquals(Brilliance.DECENT, emptied.brilliance());
        assertEquals(new MindPool(0L, 7_000L, false), emptied.getPool(MindPoolType.THOUGHTS));
        assertTrue(emptied.taggedThoughts().isEmpty());
    }
}
