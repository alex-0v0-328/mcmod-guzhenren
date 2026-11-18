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
 * 区块地形回溯还原服务：将山如故[Mountain As Before Gu]与江如故[River As Before Gu]作用范围内的地面恢复为初始裸地形。
 * <ul>
 *   <li>{@link #rewindAround} 调用 {@link ChunkOriginGenerator} 在后台重生成半径 {@link #RESTORE_RADIUS}（5×5 方阵）的
 *       初始裸地形；生成完毕（约 1–3 秒）后回到服务端主线程将目标区块加入队列（{@link #enqueue}），由
 *       {@link #tickQueue} 每服务端 tick 取出 1 个区块执行 {@link #restore} 还原。</li>
 *   <li>{@link #restore} 逐格比对当前区块与裸地形区块，仅对状态不同的位置以 {@link #RESTORE_FLAGS}（同步客户端、抑制方
 *       块掉落且不触发邻居形状更新）写入原貌方块：非裸地形的树木、建筑与玩家放置方块均消失（容器按原版移除方块实体逻辑
 *       洒出内部物品），而生物实体、掉落物与生物群系保持不变。</li>
 *   <li>⚠ 写入时抑制邻居更新，因此方阵边界被截断的树木外侧半边会保留枝叶，填平坑洞旁的水流也会保持静止直至下次受扰
 *       动。</li>
 *   <li>⚠ 待还原队列与计数器（{@code generating}、{@code epoch}）为全服共享的内存静态状态：停服时 {@link #clear} 清空剩
 *       余队列并递增 {@code epoch} 纪元号，使停服后才完成的后台生成任务自动丢弃；后台生成若因第三方世界生成模组异常失败
 *       则仅记录错误日志。</li>
 * </ul>
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
