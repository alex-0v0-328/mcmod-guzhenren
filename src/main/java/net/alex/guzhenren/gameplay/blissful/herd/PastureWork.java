package net.alex.guzhenren.gameplay.blissful.herd;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵面板「养殖」页的牧场：栏内栏外、「圈地」放栅栏、「收拢」把栏外动物传进栏，以及牧场强加载。
 * <ul>
 *   <li>牧场 = {@link BlissfulLand#pastureMin()} 起 {@code pastureChunks} 见方的区块（{@link Pasture#of}）。在不在栏内只
 *       看实体坐标落没落在这块方格里，不看栅栏；栅栏只管把动物关在里面。</li>
 *   <li>养殖的清点、托管、一键喂养与宰杀只看栏内（{@link #penned}）；总览页的动物数照旧数全福地。「挪生物上山」跳过栏内
 *       的动物（{@link MountainWork#moveMobsUp}）。</li>
 *   <li>{@link #enclose}：沿方格最外一圈，每列真实地面（找地面时栅栏与栅栏门不算，否则再按会在栅栏上叠一层）上一格放
 *       橡木栅栏，北边正中一格（{@link Pasture#gate}）放朝北的栅栏
 *       门；已是栅栏或栅栏门的格子跳过，可替换方块、树叶、原木直接换掉，其余方块（多半是玩家的）不动、计为挡住，流体列也
 *       计为挡住。全部放完再按四邻重算一遍形状，栅栏才连得上。改地形先过 {@link ModEvents#canGrief}；再按一次只补缺的。
 *       </li>
 *   <li>{@link #gather}：候选里栏外的可支使动物（同养殖清点：原版 {@link Animal} 且 {@link MountainWork#isManaged}），至
 *       多 {@value #GATHER_LIMIT} 头，传到栏内离栅栏至少 {@value #MARGIN} 格的随机地面（每头至多试
 *       {@value #SPOT_ATTEMPTS} 次，须已加载），之后冷却 {@value #GATHER_COOLDOWN_TICKS} 刻。野生蛊不是 {@link Animal}，
 *       不会被收。</li>
 *   <li>⚠ 强加载同田区，用 NeoForge 带刻工单 {@link #TICKETS}（原版强加载的区块附近没人时不随机刻）：第一次圈地才挂，失主时
 *       {@link #release} 撤掉，认主时 {@link #retain} 重挂（圈过地才挂）；读档校验把无主福地的工单全部丢掉。</li>
 *   <li>{@link #enclose}、{@link #gather} 接受显式的 {@link Pasture}；GameTest 用测试区里的小方格调用，不碰别的测试。</li>
 * </ul>
 */

public final class PastureWork {

    private PastureWork() {}

    public static final TicketController TICKETS = new TicketController(Guzhenren.id("blissful_land_pasture"),
            PastureWork::validateTickets);
    public static final int GATHER_LIMIT = 16;
    public static final int GATHER_COOLDOWN_TICKS = 60 * Ticks.SECOND;
    public static final int MARGIN = 2;
    private static final int SPOT_ATTEMPTS = 8;
    private static final int NO_GROUND = Integer.MIN_VALUE;

    public record Pasture(int minX, int minZ, int maxX, int maxZ) {

        public static @NotNull Pasture of(@NotNull BlissfulLand land) {
            int side = land.pastureChunks() * 16;
            int x = land.pastureMin().getMinBlockX();
            int z = land.pastureMin().getMinBlockZ();
            return new Pasture(x, z, x + side - 1, z + side - 1);
        }

        public boolean contains(@NotNull Entity entity) {
            return entity.getX() >= minX && entity.getX() < maxX + 1 && entity.getZ() >= minZ
                    && entity.getZ() < maxZ + 1;
        }

        public int width() { return maxX - minX + 1; }

        public int depth() { return maxZ - minZ + 1; }

        public @NotNull BlockPos gate() { return new BlockPos((minX + maxX) / 2, 0, minZ); }

        public @NotNull BlockPos center() { return new BlockPos((minX + maxX) / 2, 0, (minZ + maxZ) / 2); }

        public @NotNull List<ChunkPos> chunks() {
            List<ChunkPos> chunks = new ArrayList<>();
            for (int x = minX >> 4; x <= maxX >> 4; x++) {
                for (int z = minZ >> 4; z <= maxZ >> 4; z++) chunks.add(new ChunkPos(x, z));
            }
            return chunks;
        }

        private List<BlockPos> ring() {
            List<BlockPos> ring = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                ring.add(new BlockPos(x, 0, minZ));
                ring.add(new BlockPos(x, 0, maxZ));
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                ring.add(new BlockPos(minX, 0, z));
                ring.add(new BlockPos(maxX, 0, z));
            }
            return ring;
        }
    }

    public record Enclosed(int placed, int blocked) {}

    //region Pen [栏内栏外]
    public static @NotNull Map<ResourceLocation, List<Animal>> penned(
            @NotNull Map<ResourceLocation, List<Animal>> herds, @NotNull Pasture pasture) {
        Map<ResourceLocation, List<Animal>> penned = new LinkedHashMap<>();
        herds.forEach((type, animals) -> {
            List<Animal> inside = new ArrayList<>();
            for (Animal animal : animals) {
                if (pasture.contains(animal)) inside.add(animal);
            }
            if (!inside.isEmpty()) penned.put(type, inside);
        });
        return penned;
    }

    public static int count(@NotNull Map<ResourceLocation, List<Animal>> herds) {
        int count = 0;
        for (List<Animal> animals : herds.values()) count += animals.size();
        return count;
    }
    //endregion

    //region Enclose [圈地]
    public static @Nullable Enclosed enclose(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                             @NotNull Pasture pasture, @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        for (ChunkPos chunk : pasture.chunks()) level.getChunk(chunk.x, chunk.z);
        BlockPos gate = pasture.gate();
        List<BlockPos> placed = new ArrayList<>();
        int blocked = 0;
        for (BlockPos column : pasture.ring()) {
            int ground = groundY(level, column.getX(), column.getZ());
            if (ground == NO_GROUND) {
                blocked++;
                continue;
            }
            BlockPos pos = new BlockPos(column.getX(), ground + 1, column.getZ());
            BlockState at = level.getBlockState(pos);
            if (at.is(BlockTags.FENCES) || at.is(BlockTags.FENCE_GATES)) continue;
            if (!at.canBeReplaced() && !at.is(BlockTags.LEAVES) && !at.is(BlockTags.LOGS)) {
                blocked++;
                continue;
            }
            boolean isGate = column.getX() == gate.getX() && column.getZ() == gate.getZ();
            BlockState state = isGate
                    ? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH)
                    : Blocks.OAK_FENCE.defaultBlockState();
            level.setBlock(pos, state, Block.UPDATE_ALL);
            placed.add(pos);
        }
        for (BlockPos pos : placed) {
            BlockState joined = Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos);
            level.setBlock(pos, joined, Block.UPDATE_ALL);
        }
        data.herd().setEnclosed();
        forceChunks(level, pasture, true);
        return new Enclosed(placed.size(), blocked);
    }

    private static int groundY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x,
                z) - 1, z);
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return NO_GROUND;
            if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()
                    || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES)) {
                continue;
            }
            return pos.getY();
        }
        return NO_GROUND;
    }
    //endregion

    //region Gather [收拢]
    public static long gatherCooldown(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return Math.max(0L, data.herd().gatherReadyAt() - level.getGameTime());
    }

    public static int gather(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull Pasture pasture,
                             @NotNull RandomSource random, @NotNull Iterable<? extends Entity> candidates) {
        List<Animal> movers = new ArrayList<>();
        for (Entity entity : candidates) {
            if (movers.size() >= GATHER_LIMIT) break;
            if (entity instanceof Animal animal && MountainWork.isManaged(animal) && !pasture.contains(animal)) {
                movers.add(animal);
            }
        }
        int moved = 0;
        for (Animal animal : movers) {
            BlockPos spot = findSpot(level, pasture, random);
            if (spot == null) continue;
            animal.teleportTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            animal.setDeltaMovement(Vec3.ZERO);
            moved++;
        }
        data.herd().setGatherReadyAt(level.getGameTime() + GATHER_COOLDOWN_TICKS);
        return moved;
    }

    private static @Nullable BlockPos findSpot(ServerLevel level, Pasture pasture, RandomSource random) {
        int spanX = pasture.width() - 2 * MARGIN;
        int spanZ = pasture.depth() - 2 * MARGIN;
        if (spanX <= 0 || spanZ <= 0) return null;
        for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
            int x = pasture.minX() + MARGIN + random.nextInt(spanX);
            int z = pasture.minZ() + MARGIN + random.nextInt(spanZ);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int ground = groundY(level, x, z);
            if (ground == NO_GROUND) continue;
            BlockPos spot = new BlockPos(x, ground + 1, z);
            if (level.getBlockState(spot).canBeReplaced() && level.getBlockState(spot.above()).canBeReplaced()) {
                return spot;
            }
        }
        return null;
    }
    //endregion

    //region Tickets [强加载]
    public static void retain(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        if (data.herd().enclosed()) forceChunks(level, Pasture.of(data.land()), true);
    }

    public static void release(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        forceChunks(level, Pasture.of(data.land()), false);
    }

    public static void forceChunks(@NotNull ServerLevel level, @NotNull Pasture pasture, boolean add) {
        BlockPos owner = pasture.center();
        for (ChunkPos chunk : pasture.chunks()) TICKETS.forceChunk(level, owner, chunk.x, chunk.z, add, true);
    }

    private static void validateTickets(ServerLevel level, TicketHelper tickets) {
        BlissfulLand land = BlissfulLands.landOf(level);
        if (land != null && BlissfulLandData.get(level.getServer(), land).hasOwner()) return;
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) tickets.removeAllTickets(owner);
    }
    //endregion
}
