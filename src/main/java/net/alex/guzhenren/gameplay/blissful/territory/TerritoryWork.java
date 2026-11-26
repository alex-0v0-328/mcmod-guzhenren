package net.alex.guzhenren.gameplay.blissful.territory;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.blissful.farm.FieldPlot;
import net.alex.guzhenren.gameplay.blissful.herd.PastureWork;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.land.Territory;
import net.alex.guzhenren.gameplay.blissful.map.Siting;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵面板「疆域」页：永久割弃一侧条带，并把割弃区块清成虚空。
 * <ul>
 *   <li>{@link #cede} 先作废条带内的标准田（撤强加载）、规划与牧场（任一区块落在条带里即作废，撤强加载；地形随后清成
 *       虚空，不还原），再改疆域记录（{@link Territory#cede}），把条带内的玩家送回各自返回点、非玩家实体直接移除（不掉
 *       落），并把条带里已加载的区块排队清空；没加载的区块等加载时由 {@link #onChunkLoad} 排队。Gu World 生成器不改，所
 *       以割弃区块每次生成或读回都要再清一次，已全空的跳过。</li>
 *   <li>{@link #tick} 每刻至多换 {@value #BLOCKS_PER_TICK} 个方块：逐格把非空气方块换成空气，标志同如故类还原（同步客户
 *       端、不掉落、不触发邻居形状更新），全空的区段整段跳过；一个区块清不完，游标记在队列里下一刻接着清；清完再移除区
 *       块内的非玩家实体（容器洒出的物品一并移除）、送走玩家。</li>
 *   <li>不整段替换区块段：那样要自己补光照、兴趣点、方块实体与区块重发，逐格走 {@code setBlock} 让原版处理这些，代价由
 *       每刻预算摊开（一个满区块约 3 万格，八刻左右清完）。</li>
 *   <li>原版边界墙只能是正方形，割弃那一侧没有墙；掉进虚空由 {@link BlissfulLands} 拉回入口。</li>
 *   <li>⚠ 待清队列是按维度的内存静态状态，停服由 {@link #clear} 清空；漏清的区块下次加载会再排队，不会留下地形。</li>
 *   <li>⚠ 性能：玩家附近已加载的条带区块按服务端视距估算，不遍历整条割弃带。</li>
 * </ul>
 */

public final class TerritoryWork {

    private TerritoryWork() {}

    public static final int BLOCKS_PER_TICK = 4096;
    private static final int SECTION_BLOCKS = 4096;
    private static final int DONE = -1;
    public static final int VOID_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<ResourceKey<Level>, Queue> QUEUES = new HashMap<>();

    private static final class Queue {

        private final LongArrayFIFOQueue order = new LongArrayFIFOQueue();
        private final LongSet pending = new LongOpenHashSet();
        private long current = Long.MIN_VALUE;
        private int cursor;
    }

    public record Voided(int fields, int plans, int pastures) {}

    public static @Nullable Voided cede(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                        @NotNull Territory.Side side, int width) {
        return cede(level, data, side, width, level.getAllEntities());
    }

    public static @Nullable Voided cede(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                        @NotNull Territory.Side side, int width,
                                        @NotNull Iterable<? extends Entity> entities) {
        Territory before = data.territory();
        if (!before.canCede(side, width)) return null;

        Territory after = before.cede(side, width);
        Voided voided = voidHoldings(level, data, after);
        data.setTerritory(after);
        List<Entity> inside = new ArrayList<>();
        for (Entity entity : entities) {
            if (after.isCeded(entity.chunkPosition().x, entity.chunkPosition().z)) inside.add(entity);
        }
        for (Entity entity : inside) expel(entity);
        int view = level.getServer().getPlayerList().getViewDistance() + 1;
        for (ServerPlayer player : level.players()) {
            ChunkPos at = player.chunkPosition();
            for (int x = at.x - view; x <= at.x + view; x++) {
                for (int z = at.z - view; z <= at.z + view; z++) {
                    if (after.isCeded(x, z) && level.hasChunk(x, z)) enqueue(level, x, z);
                }
            }
        }
        return voided;
    }

    private static Voided voidHoldings(ServerLevel level, BlissfulLandData data, Territory after) {
        int fields = 0;
        for (FieldPlot field : data.farm().fields()) {
            if (!after.isCeded(field.chunk().x, field.chunk().z)) continue;
            FarmWork.setForced(level, field, false);
            data.farm().removeField(field);
            fields++;
        }
        int plans = 0;
        for (long plan : data.farm().plans()) {
            if (after.isCeded(ChunkPos.getX(plan), ChunkPos.getZ(plan)) && data.farm().removePlan(plan)) plans++;
        }
        int pastures = 0;
        List<ChunkPos> corners = data.herd().pastures();
        for (int i = corners.size() - 1; i >= 0; i--) {
            ChunkPos corner = corners.get(i);
            boolean ceded = false;
            for (int dx = 0; dx < Siting.PEN_CHUNKS; dx++) {
                for (int dz = 0; dz < Siting.PEN_CHUNKS; dz++) ceded |= after.isCeded(corner.x + dx, corner.z + dz);
            }
            if (!ceded) continue;
            PastureWork.forceChunks(level, PastureWork.Pasture.at(corner.x, corner.z), false);
            data.herd().removePasture(i);
            pastures++;
        }
        return new Voided(fields, plans, pastures);
    }

    public static void onChunkLoad(@NotNull ServerLevel level, @NotNull Territory territory,
                                   @NotNull LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        if (territory.isCeded(pos.x, pos.z) && !isEmpty(chunk)) enqueue(level, pos.x, pos.z);
    }

    public static void tick(@NotNull ServerLevel level) {
        Queue queue = QUEUES.get(level.dimension());
        if (queue == null) return;

        int budget = BLOCKS_PER_TICK;
        while (budget > 0) {
            if (queue.current == Long.MIN_VALUE) {
                if (queue.order.isEmpty()) return;
                queue.current = queue.order.dequeueLong();
                queue.cursor = 0;
            }
            int x = ChunkPos.getX(queue.current);
            int z = ChunkPos.getZ(queue.current);
            if (!level.hasChunk(x, z)) {
                finish(queue);
                continue;
            }
            LevelChunk chunk = level.getChunk(x, z);
            int[] spent = {0};
            queue.cursor = voidStep(level, chunk, queue.cursor, budget, spent);
            budget -= Math.max(1, spent[0]);
            if (queue.cursor == DONE) {
                expelAll(level, chunk);
                finish(queue);
            }
        }
    }

    private static void finish(Queue queue) {
        queue.pending.remove(queue.current);
        queue.current = Long.MIN_VALUE;
        queue.cursor = 0;
    }

    public static boolean isQueued(@NotNull ServerLevel level, int chunkX, int chunkZ) {
        Queue queue = QUEUES.get(level.dimension());
        return queue != null && queue.pending.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static void clear() { QUEUES.clear(); }

    private static void enqueue(ServerLevel level, int chunkX, int chunkZ) {
        Queue queue = QUEUES.computeIfAbsent(level.dimension(), key -> new Queue());
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        if (queue.pending.add(packed)) queue.order.enqueue(packed);
    }

    private static int voidStep(ServerLevel level, LevelChunk chunk, int cursor, int budget, int[] spent) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        int end = sections.length * SECTION_BLOCKS;
        while (cursor < end) {
            int index = sections.length - 1 - cursor / SECTION_BLOCKS;
            if (sections[index].hasOnlyAir()) {
                cursor = (cursor / SECTION_BLOCKS + 1) * SECTION_BLOCKS;
                continue;
            }
            if (spent[0] >= budget) return cursor;
            int local = cursor % SECTION_BLOCKS;
            int y = (chunk.getSectionYFromSectionIndex(index) << 4) + 15 - local / 256;
            pos.set(chunk.getPos().getBlockX((local / 16) % 16), y, chunk.getPos().getBlockZ(local % 16));
            if (!chunk.getBlockState(pos).isAir()) {
                level.setBlock(pos, air, VOID_FLAGS);
                spent[0]++;
            }
            cursor++;
        }
        return DONE;
    }

    private static void expelAll(ServerLevel level, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        AABB box = new AABB(pos.getMinBlockX(), level.getMinBuildHeight() - 64, pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) expel(entity);
    }

    private static boolean isEmpty(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) return false;
        }
        return true;
    }

    private static void expel(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            DimensionTravelService.exit(player);
        } else if (!entity.isRemoved()) {
            entity.discard();
        }
    }
}
