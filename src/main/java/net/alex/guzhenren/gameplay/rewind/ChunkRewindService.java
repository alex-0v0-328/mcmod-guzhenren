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
 * 把地面放回裸地形，供山如故[Mountain As Before Gu]、江如故[River As Before Gu]使用。
 * <ul>
 *   <li>{@link #rewindAround} 让 {@link ChunkOriginGenerator} 从种子重生成 {@link #RESTORE_RADIUS} 方阵（5×5，只有地
 *       形），生成完（1–3 秒）后经 {@link #enqueue}、{@link #tickQueue} 每服务端 tick 以临时孪生区块还原一个请求区块
 *       （{@link #restore}）；{@link #getGenerating} 计尚未入队的生成。</li>
 *   <li>逐位置对比原貌（约 98,000 次，几毫秒），只改不同处，{@link #RESTORE_FLAGS} 更新客户端、不更新邻居、不掉落，不引
 *       发连锁；非裸地形的树、建筑、放置方块都消失，容器按原版洒出物品；实体、掉落物、群系不动。</li>
 *   <li>⚠ 不更新邻居：方阵边缘被切的树留外半边连叶；填平坑旁的水静止，直到被更新。</li>
 *   <li>⚠ 队列静态且临时：停服时 {@link #clear} 丢弃剩余（最多 25 区块）、忘掉运行中生成，停服后才完成的生成与上个服务
 *       器遗留的队列项（下个服务器首 tick）都被丢弃；抛异常的生成（损坏的世界生成模组）记日志，不还原。</li>
 *   <li>可变静态 {@code generating}、{@code epoch} 有意全服共享，计唯一运行服务器上的进行中生成；停服时 {@link #clear}
 *       归零并递增纪元，丢弃迟到的完成。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkOriginGenerator
 * @see ChunkKind
 * @since 1.0.0
 */

public final class ChunkRewindService {

    private ChunkRewindService() {}

    public static final int RESTORE_RADIUS = 2;
    public static final int RESTORE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Deque<PendingRestore> QUEUE = new ArrayDeque<>();
    private static int generating;
    private static int epoch;

    //region generating [生成]
    public static void rewindAround(@NotNull ServerLevel level, @NotNull ChunkPos center,
                                    @NotNull Collection<ChunkPos> targets) {
        if (targets.isEmpty()) return;

        MinecraftServer server = level.getServer();
        int started = beginGeneration();
        ChunkOriginGenerator.generate(level, center, RESTORE_RADIUS).whenComplete((origins, error) ->
                server.execute(() -> {
                    if (!endGeneration(started)) return;
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

    public static int getGenerating() { return generating; }

    static int beginGeneration() {
        generating++;
        return epoch;
    }

    static boolean endGeneration(int started) {
        if (started != epoch) return false;

        generating--;
        return true;
    }

    static void clear() {
        QUEUE.clear();
        generating = 0;
        epoch++;
    }
    //endregion

    //region restoring [还原]
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

    //region the queue [队列]
    static void enqueue(@NotNull ServerLevel level, @NotNull ChunkPos pos, @NotNull ChunkAccess origin) {
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
