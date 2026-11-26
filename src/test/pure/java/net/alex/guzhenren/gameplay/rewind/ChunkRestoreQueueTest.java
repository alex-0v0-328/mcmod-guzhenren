package net.alex.guzhenren.gameplay.rewind;

import java.util.List;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 分摊还原的预算与次序：每 tick 区段数取 ceil(总数 / 160)，区块按切比雪夫距离自中心一圈圈向外、同圈保持传入次序。 */

class ChunkRestoreQueueTest {

    @Test
    @DisplayName("an overworld cast of 25 chunks restores 4 sections a tick and ends in 150 ticks")
    void theOverworldBudgetIsFourSectionsForOneHundredFiftyTicks() {
        int sections = 25 * 24;
        int budget = ChunkRestoreQueue.getSectionsPerTick(sections);

        assertEquals(4, budget);
        assertEquals(16_384, budget * 16 * 16 * 16);
        assertEquals(150, Math.ceilDiv(sections, budget));
    }

    @Test
    @DisplayName("the budget rounds up so a cast never runs past 160 ticks and always moves")
    void theBudgetRoundsUp() {
        assertEquals(1, ChunkRestoreQueue.getSectionsPerTick(1));
        assertEquals(1, ChunkRestoreQueue.getSectionsPerTick(160));
        assertEquals(2, ChunkRestoreQueue.getSectionsPerTick(161));
        assertEquals(2, ChunkRestoreQueue.getSectionsPerTick(25 * 8));
    }

    @Test
    @DisplayName("chunks go ring by ring out from the centre and keep their order within a ring")
    void chunksGoRingByRing() {
        ChunkPos center = new ChunkPos(10, -4);
        List<ChunkPos> targets = List.of(new ChunkPos(8, -6), new ChunkPos(11, -4), new ChunkPos(10, -2),
                new ChunkPos(10, -4), new ChunkPos(9, -5), new ChunkPos(12, -3));

        List<ChunkPos> ordered = ChunkRestoreQueue.inRings(center, targets);

        assertEquals(List.of(new ChunkPos(10, -4), new ChunkPos(11, -4), new ChunkPos(9, -5), new ChunkPos(8, -6),
                new ChunkPos(10, -2), new ChunkPos(12, -3)), ordered);
    }
}
