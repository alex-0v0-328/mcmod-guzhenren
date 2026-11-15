package net.alex.guzhenren.gameplay.rewind;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 服务端停止时不得残留的静态恢复状态：排队区块持其世界引用；运行中的生成计数会让下个服务端仍大于零。 */

class ChunkRewindServiceTest {

    @AfterEach
    void reset() { ChunkRewindService.clear(); }

    @Test
    @DisplayName("clearing on a server stop empties the queue and the generating count")
    void clearEmptiesTheQueueAndTheCount() {
        ChunkRewindService.enqueue(null, new ChunkPos(0, 0), null);
        ChunkRewindService.enqueue(null, new ChunkPos(1, 0), null);
        ChunkRewindService.beginGeneration();
        assertEquals(2, ChunkRewindService.getQueued());
        assertEquals(1, ChunkRewindService.getGenerating());

        ChunkRewindService.clear();

        assertEquals(0, ChunkRewindService.getQueued());
        assertEquals(0, ChunkRewindService.getGenerating());
    }

    @Test
    @DisplayName("a generation of the stopped server finishing late is dropped and cannot push the count below zero")
    void aLateGenerationIsDropped() {
        int stale = ChunkRewindService.beginGeneration();
        ChunkRewindService.clear();
        int fresh = ChunkRewindService.beginGeneration();

        assertFalse(ChunkRewindService.endGeneration(stale));
        assertEquals(1, ChunkRewindService.getGenerating());
        assertTrue(ChunkRewindService.endGeneration(fresh));
        assertEquals(0, ChunkRewindService.getGenerating());
    }
}
