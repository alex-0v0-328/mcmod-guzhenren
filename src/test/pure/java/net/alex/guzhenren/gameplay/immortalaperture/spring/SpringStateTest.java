package net.alex.guzhenren.gameplay.immortalaperture.spring;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.concurrent.atomic.AtomicInteger;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringStateTest {

    private static final StackIo NO_STACKS = new StackIo() {

        @Override
        public @NotNull Tag save(@NotNull ItemStack stack) { throw new AssertionError("springs hold no stacks"); }

        @Override
        public @NotNull ItemStack load(@NotNull CompoundTag tag) { throw new AssertionError("springs hold no stacks"); }
    };
    private final AtomicInteger dirty = new AtomicInteger();
    private final SpringState state = new SpringState(dirty::incrementAndGet);

    private static LongList packed(BlockPos... positions) {
        LongList list = new LongArrayList();
        for (BlockPos pos : positions) list.add(pos.asLong());
        return list;
    }

    @Test
    @DisplayName("adding a spring twice counts it once and marks the land dirty once")
    void addIsIdempotent() {
        BlockPos spring = new BlockPos(20, 70, -9);
        assertTrue(state.add(spring));
        assertFalse(state.add(spring));
        assertEquals(1, state.count());
        assertEquals(1, dirty.get());
        assertTrue(state.contains(spring));
    }

    @Test
    @DisplayName("removing a spring drops it and an unknown one changes nothing")
    void removeForgets() {
        BlockPos spring = new BlockPos(20, 70, -9);
        state.add(spring);
        assertTrue(state.remove(spring));
        assertFalse(state.remove(spring));
        assertEquals(0, state.count());
        assertFalse(state.contains(spring));
        assertEquals(2, dirty.get());
    }

    @Test
    @DisplayName("rescanning a chunk replaces only that chunk's springs")
    void replaceChunkTouchesOneChunk() {
        BlockPos here = new BlockPos(3, 64, 3);
        BlockPos other = new BlockPos(40, 64, 3);
        state.add(here);
        state.add(other);

        state.replaceChunk(new ChunkPos(0, 0), packed(new BlockPos(5, 66, 7), new BlockPos(9, 66, 9)));
        assertFalse(state.contains(here));
        assertTrue(state.contains(other));
        assertEquals(3, state.count());

        state.replaceChunk(new ChunkPos(0, 0), new LongArrayList());
        assertEquals(1, state.count());
        assertTrue(state.contains(other));
    }

    @Test
    @DisplayName("rescanning a chunk with the same springs, or an empty chunk, does not dirty the save")
    void unchangedRescanStaysClean() {
        state.replaceChunk(new ChunkPos(1, 1), new LongArrayList());
        assertEquals(0, dirty.get());

        LongList found = packed(new BlockPos(18, 64, 18));
        state.replaceChunk(new ChunkPos(1, 1), found);
        int afterFirst = dirty.get();
        state.replaceChunk(new ChunkPos(1, 1), found);
        assertEquals(afterFirst, dirty.get());
        assertEquals(1, state.count());
    }

    @Test
    @DisplayName("the position snapshot survives removing springs while walking it")
    void snapshotIsStable() {
        for (int i = 0; i < 5; i++) state.add(new BlockPos(i * 32, 64, 0));
        for (long position : state.positions()) state.remove(BlockPos.of(position));
        assertEquals(0, state.count());
        assertTrue(state.positions().isEmpty());
    }

    @Test
    @DisplayName("springs round trip through the save, negative coordinates included")
    void saveRoundTrip() {
        state.add(new BlockPos(-37, 71, -1024));
        state.add(new BlockPos(5000, 63, 12));
        CompoundTag tag = new CompoundTag();
        state.save(tag, "springs", NO_STACKS);

        SpringState loaded = new SpringState(() -> {});
        loaded.load(tag, "springs", NO_STACKS);
        assertEquals(2, loaded.count());
        assertTrue(loaded.contains(new BlockPos(-37, 71, -1024)));
        assertTrue(loaded.contains(new BlockPos(5000, 63, 12)));
    }

    @Test
    @DisplayName("a save with no springs entry loads as empty")
    void missingEntryLoadsEmpty() {
        state.load(new CompoundTag(), "springs", NO_STACKS);
        assertEquals(0, state.count());
    }
}
