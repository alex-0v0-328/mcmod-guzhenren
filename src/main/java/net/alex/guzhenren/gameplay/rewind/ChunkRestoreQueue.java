package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 区块地形还原的分摊队列：把山如故[Mountain As Before Gu]或江如故[River As Before Gu]一次催动的还原按 16×16×16 区段
 * 摊到至多 {@link #RESTORE_TICKS} tick（8 秒）内写回。
 * <ul>
 *   <li>一次催动为一批（{@link #add}），每 tick 还原区段数 = ceil(本批区段总数 / {@link #RESTORE_TICKS})
 *       （{@link #getSectionsPerTick}）：主世界 25 区块 × 24 区段 = 600，每 tick 4 区段（比对 16,384 格），150 tick
 *       （7.5 秒）还原完。</li>
 *   <li>区块按与玩家脚下中心区块的切比雪夫距离一圈圈向外（{@link #inRings}，同圈保持传入次序）；区块内自最低区段向上：
 *       写入方块即调用 {@code onPlace}，沙砾随之排定下落、流体排定流动，自下而上保证每格下方在本区块内已是原貌，不会
 *       落进或流进尚未填回的坑，与一次性还原（{@link ChunkRewindService#restore}）结果一致；拆除支撑因
 *       {@link ChunkRewindService#RESTORE_FLAGS} 不发邻居更新，上方方块不会因此坠落。</li>
 *   <li>多批按生成完成先后先进先出（{@link #tick}），只还原队首一批，队首完成的那个 tick 不再开下一批；后台生成时间不计
 *       入 8 秒。</li>
 *   <li>豁免保留位（{@link ChunkRewindService#findKept}）在区块第一个区段前算一次，缓存到该区块处理完；区块中途卸载
 *       （{@code getChunkNow} 不再返回同一对象）时跳过其余区段，跳过的区段不占预算。</li>
 *   <li>⚠ 批次持有维度、区块与裸地形引用：停服时须经 {@link #clear} 丢弃；队首维度不属于当前服务端（单人切世界）时整队
 *       清空。</li>
 * </ul>
 */

public final class ChunkRestoreQueue {

    public static final int RESTORE_TICKS = 160;
    private final Deque<Batch> batches = new ArrayDeque<>();

    public void add(@NotNull ServerLevel level, @NotNull ChunkPos center, @NotNull Collection<ChunkPos> targets,
                    @NotNull Map<ChunkPos, ChunkAccess> origins) {
        List<Target> ordered = new ArrayList<>();
        int sections = 0;
        for (ChunkPos pos : inRings(center, targets)) {
            ChunkAccess origin = origins.get(pos);
            if (origin == null) continue;

            ordered.add(new Target(pos, origin));
            sections += origin.getSectionsCount();
        }
        if (!ordered.isEmpty()) batches.addLast(new Batch(level, ordered, getSectionsPerTick(sections)));
    }

    public int tick(@NotNull MinecraftServer server) {
        Batch head = batches.peekFirst();
        if (head == null) return 0;
        if (head.level.getServer() != server) {
            clear();
            return 0;
        }
        int restored = head.tick();
        if (head.isDone()) batches.pollFirst();
        return restored;
    }

    public int getQueued() { return batches.size(); }

    public void clear() { batches.clear(); }

    public static int getSectionsPerTick(int sections) { return Math.max(1, Math.ceilDiv(sections, RESTORE_TICKS)); }

    static @NotNull List<ChunkPos> inRings(@NotNull ChunkPos center, @NotNull Collection<ChunkPos> targets) {
        List<ChunkPos> ordered = new ArrayList<>(targets);
        ordered.sort(Comparator.comparingInt(pos -> center.getChessboardDistance(pos)));
        return ordered;
    }

    private record Target(ChunkPos pos, ChunkAccess origin) {}

    private static final class Batch {

        private final ServerLevel level;
        private final List<Target> targets;
        private final int perTick;
        private int next;
        private int section;
        private int sections;
        private @Nullable LevelChunk chunk;
        private @Nullable BitSet kept;

        private Batch(ServerLevel level, List<Target> targets, int perTick) {
            this.level = level;
            this.targets = targets;
            this.perTick = perTick;
        }

        private int tick() {
            int restored = 0;
            while (restored < perTick && !isDone()) {
                Target target = targets.get(next);
                LevelChunk live = level.getChunkSource().getChunkNow(target.pos().x, target.pos().z);
                if (chunk == null) {
                    if (live == null || live.getMinSection() != target.origin().getMinSection()) {
                        advance();
                        continue;
                    }
                    chunk = live;
                    kept = ChunkRewindService.findKept(live);
                    sections = Math.min(live.getSectionsCount(), target.origin().getSectionsCount());
                } else if (live != chunk) {
                    advance();
                    continue;
                }
                ChunkRewindService.restoreSection(level, chunk, section,
                        target.origin().getSection(section).getStates(), kept);
                restored++;
                if (++section >= sections) advance();
            }
            return restored;
        }

        private boolean isDone() { return next >= targets.size(); }

        private void advance() {
            next++;
            section = 0;
            chunk = null;
            kept = null;
        }
    }
}
