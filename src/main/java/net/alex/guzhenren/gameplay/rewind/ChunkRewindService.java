package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Puts the land back to its bare landform, for the Mountain and River As Before Gu [山如故 / 江如故]:
 * {@link #rewindAround} has {@link ChunkOriginGenerator} generate the {@link #RESTORE_RADIUS} square (3×3) again
 * from the seed -- terrain only, no trees, plants, lakes, snow, ice, ores or structures -- then restores each chunk
 * asked for from its scratch twin ({@link #restore}).
 *
 * <p>A restore walks every position of the chunk against the origin (≈98,000 compares, a few milliseconds) and
 * sets only what differs, with {@link #RESTORE_FLAGS}: clients updated, no neighbor updates, no drops -- the land
 * is put back, nothing cascades from it. Whatever stands in the chunk and is not bare land goes: trees, buildings,
 * placed blocks. A container that goes spills its items the vanilla way. Entities, dropped items and biomes are not
 * touched. The chunks go through {@link #enqueue} and {@link #tickQueue}, one per server tick, once generation
 * finishes in the background (one to three seconds).
 *
 * <p>⚠ No neighbor updates also means a tree cut at the square's edge keeps its outer half, leaves included, and
 * water beside a refilled hole stays still until something updates it. ⚠ The restore queue is static and
 * transient: a server stop drops what is left (at most nine chunks), a generation that finishes after its server
 * stopped is dropped, and a queued entry from an earlier server is dropped on the first tick of the next. A
 * generation that throws (a broken worldgen mod) is logged and restores nothing.
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkOriginGenerator
 * @see ChunkKind
 * @since 1.0.0
 */

public final class ChunkRewindService {

    private ChunkRewindService() {}

    public static final int RESTORE_RADIUS = 1;
    public static final int RESTORE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Deque<PendingRestore> QUEUE = new ArrayDeque<>();
    private static int generating;

    //region generating
    /** Generates the square around {@code center} again and queues the {@code targets} inside it for restoring. */
    public static void rewindAround(@NotNull ServerLevel level, @NotNull ChunkPos center,
                                    @NotNull Collection<ChunkPos> targets) {
        if (targets.isEmpty()) return;

        MinecraftServer server = level.getServer();
        generating++;
        ChunkOriginGenerator.generate(level, center, RESTORE_RADIUS).whenComplete((origins, error) ->
                server.execute(() -> {
                    generating--;
                    if (error != null) {
                        Guzhenren.LOGGER.error("As Before Gu could not generate the land around {} again", center,
                                error);
                        return;
                    }
                    for (ChunkPos pos : targets) {
                        ChunkAccess origin = origins.get(pos);
                        if (origin != null) enqueue(level, pos, origin);
                    }
                }));
    }

    /** Generations still running in the background; their chunks are not queued yet. */
    public static int getGenerating() { return generating; }
    //endregion

    //region restoring
    public static int restore(@NotNull ServerLevel level, @NotNull LevelChunk chunk, @NotNull ChunkAccess origin) {
        if (origin.getMinSection() != chunk.getMinSection()) return 0;

        int changed = 0;
        int count = Math.min(chunk.getSectionsCount(), origin.getSectionsCount());
        for (int index = 0; index < count; index++) {
            changed += restoreSection(level, chunk, index, origin.getSection(index).getStates());
        }
        return changed;
    }

    private static int restoreSection(ServerLevel level, LevelChunk chunk, int index,
                                      PalettedContainer<BlockState> was) {
        int changed = 0;
        ChunkPos pos = chunk.getPos();
        int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    BlockState before = was.get(x, y, z);
                    if (before == chunk.getSection(index).getBlockState(x, y, z)) continue;

                    cursor.set(pos.getMinBlockX() + x, minY + y, pos.getMinBlockZ() + z);
                    if (level.setBlock(cursor, before, RESTORE_FLAGS)) changed++;
                }
            }
        }
        return changed;
    }
    //endregion

    //region the queue -- one chunk a tick
    public static void enqueue(@NotNull ServerLevel level, @NotNull ChunkPos pos, @NotNull ChunkAccess origin) {
        QUEUE.addLast(new PendingRestore(level, pos, origin));
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
        if (chunk != null) restore(next.level(), chunk, next.origin());
    }

    private record PendingRestore(ServerLevel level, ChunkPos pos, ChunkAccess origin) {}
    //endregion
}
