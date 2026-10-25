package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The land's memory for the Mountain and River As Before Gu [山如故 / 江如故]: records the chunks around a carrier
 * ({@link #recordAround}), finds the snapshot a day back ({@link #pick}) and puts a chunk back to it
 * ({@link #restore}).
 *
 * <p>Recording covers the {@link #RECORD_RADIUS} square around the carrier's chunk (5×5) once per
 * {@code RewindClock.INTERVAL_TICKS}; the Gu restores the {@link #RESTORE_RADIUS} square (3×3), so a carrier who
 * stood here a day ago finds every chunk remembered. A chunk records only when its own ring says it is due, so two
 * carriers over one chunk take one snapshot. Recording skips a chunk that is not loaded; it never loads one.
 *
 * <p>A restore walks every position of the chunk against the snapshot (≈98,000 compares, a few milliseconds) and
 * sets only what differs, with {@link #RESTORE_FLAGS}: clients updated, no neighbor updates, no drops -- the land
 * is put back, nothing cascades from it. A block entity is reloaded only where its block changed, and a restored
 * {@link Container} comes back empty. The nine chunks go through {@link #enqueue} and {@link #tickQueue}, one per
 * server tick.
 *
 * <p>⚠ The chunk attachment is the one place this class touches the loader; a version seam ({@link #getMemory},
 * {@link #setMemory}): the Forge 1.20.1 port keeps the same memory in a chunk capability. ⚠ The restore queue is
 * static and transient: a server stop drops what is left (at most eight chunks, under half a second), and a queued
 * entry from an earlier server is dropped on the first tick of the next.
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkSnapshot
 * @see ChunkMemory
 * @see RewindClock
 * @since 1.0.0
 */

public final class ChunkRewindService {

    private ChunkRewindService() {}

    public static final int RECORD_RADIUS = 2;
    public static final int RESTORE_RADIUS = 1;
    public static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Deque<PendingRestore> QUEUE = new ArrayDeque<>();

    //region the memory on the chunk -- the version seam
    public static @NotNull ChunkMemory getMemory(@NotNull LevelChunk chunk) {
        return chunk.getData(ModAttachments.CHUNK_MEMORY);
    }

    public static void setMemory(@NotNull LevelChunk chunk, @NotNull ChunkMemory memory) {
        chunk.setData(ModAttachments.CHUNK_MEMORY, memory);
        chunk.setUnsaved(true);
    }
    //endregion

    //region recording
    public static void recordAround(@NotNull ServerLevel level, @NotNull ChunkPos center, long now) {
        for (int x = center.x - RECORD_RADIUS; x <= center.x + RECORD_RADIUS; x++) {
            for (int z = center.z - RECORD_RADIUS; z <= center.z + RECORD_RADIUS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) record(chunk, now);
            }
        }
    }

    public static boolean record(@NotNull LevelChunk chunk, long now) {
        ChunkMemory memory = getMemory(chunk);
        if (!RewindClock.isDue(memory.getLastTakenAt(), now)) return false;

        setMemory(chunk, memory.with(ChunkSnapshot.capture(chunk, now)));
        return true;
    }

    public static @Nullable ChunkSnapshot pick(@NotNull LevelChunk chunk, long now) {
        return getMemory(chunk).pick(now);
    }
    //endregion

    //region restoring
    public static int restore(@NotNull ServerLevel level, @NotNull LevelChunk chunk, @NotNull ChunkSnapshot snapshot) {
        if (snapshot.minSection() != chunk.getMinSection()) return 0;

        Set<Long> changed = new HashSet<>();
        int count = Math.min(chunk.getSectionsCount(), snapshot.sections().size());
        for (int index = 0; index < count; index++) {
            restoreSection(level, chunk, index, snapshot.sections().get(index), changed);
        }
        for (CompoundTag tag : snapshot.blockEntities()) restoreBlockEntity(level, tag, changed);
        return changed.size();
    }

    private static void restoreSection(ServerLevel level, LevelChunk chunk, int index,
                                       PalettedContainer<BlockState> was, Set<Long> changed) {
        ChunkPos pos = chunk.getPos();
        int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    BlockState before = was.get(x, y, z);
                    if (before == chunk.getSection(index).getBlockState(x, y, z)) continue;

                    cursor.set(pos.getMinBlockX() + x, minY + y, pos.getMinBlockZ() + z);
                    if (level.setBlock(cursor, before, RESTORE_FLAGS)) changed.add(cursor.asLong());
                }
            }
        }
    }

    private static void restoreBlockEntity(ServerLevel level, CompoundTag tag, Set<Long> changed) {
        BlockPos pos = BlockEntity.getPosFromTag(tag);
        if (!changed.contains(pos.asLong())) return;

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || !tag.getString("id").equals(BlockEntityType.getKey(entity.getType()).toString())) return;

        entity.loadWithComponents(tag, level.registryAccess());
        if (entity instanceof Container container) container.clearContent();
        entity.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }
    //endregion

    //region the queue -- one chunk a tick
    public static void enqueue(@NotNull ServerLevel level, @NotNull ChunkPos pos, @NotNull ChunkSnapshot snapshot) {
        QUEUE.addLast(new PendingRestore(level, pos, snapshot));
    }

    public static int getQueued() { return QUEUE.size(); }

    public static void tickQueue(@NotNull MinecraftServer server) {
        PendingRestore next = QUEUE.pollFirst();
        if (next == null) return;
        if (next.level().getServer() != server) {
            QUEUE.clear();
            return;
        }
        LevelChunk chunk = next.level().getChunkSource().getChunkNow(next.pos().x, next.pos().z);
        if (chunk != null) restore(next.level(), chunk, next.snapshot());
    }

    private record PendingRestore(ServerLevel level, ChunkPos pos, ChunkSnapshot snapshot) {}
    //endregion
}
