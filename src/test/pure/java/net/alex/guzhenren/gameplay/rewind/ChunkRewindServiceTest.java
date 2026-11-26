package net.alex.guzhenren.gameplay.rewind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 停服不得残留的静态恢复状态：运行中的生成计数会让下个服务端仍大于零（批次清空见 ChunkRestoreQueueGameTests）。 */

class ChunkRewindServiceTest {

    @AfterEach
    void reset() { ChunkRewindService.clear(); }

    @Test
    @DisplayName("clearing on a server stop empties the queue and the generating count")
    void clearEmptiesTheQueueAndTheCount() {
        ChunkRewindService.beginGeneration();
        ChunkRewindService.beginGeneration();
        assertEquals(2, ChunkRewindService.getGenerating());

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
