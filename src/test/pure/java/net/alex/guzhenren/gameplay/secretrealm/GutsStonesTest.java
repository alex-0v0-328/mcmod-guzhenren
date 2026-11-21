package net.alex.guzhenren.gameplay.secretrealm;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GutsStonesTest {

    private static final BlockPos FIRST = new BlockPos(10, 70, -4);
    private static final BlockPos SECOND = new BlockPos(-3, 64, 22);

    @Test
    @DisplayName("the total counts stones, not cells, and follows every change")
    void totalCountsStones() {
        GutsStones stones = new GutsStones();
        stones.add(FIRST);
        stones.set(SECOND, 3);
        assertEquals(2, stones.size());
        assertEquals(4, stones.total());

        stones.set(FIRST, 2);
        assertEquals(5, stones.total());
        stones.remove(SECOND);
        assertEquals(2, stones.total());
        assertFalse(stones.contains(SECOND));
        stones.remove(SECOND);
        assertEquals(2, stones.total());
    }

    @Test
    @DisplayName("a cell holds one to three stones")
    void countIsClamped() {
        GutsStones stones = new GutsStones();
        stones.set(FIRST, 7);
        assertEquals(GutsStones.MAX_STONES, stones.count(FIRST));
        stones.set(FIRST, 0);
        assertEquals(1, stones.count(FIRST));
        assertEquals(1, stones.total());
    }

    @Test
    @DisplayName("only cells under three stones can take another")
    void onlyOpenCellsStack() {
        GutsStones stones = new GutsStones();
        RandomSource random = RandomSource.create(20261120L);
        assertNull(stones.pickStackable(random));

        stones.set(FIRST, GutsStones.MAX_STONES);
        assertNull(stones.pickStackable(random));
        stones.set(SECOND, 2);
        for (int i = 0; i < 20; i++) assertEquals(SECOND, stones.pickStackable(random));
    }
}
