package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地牧场：栏内栏外、圈地叠栅栏、拆栅栏、迁移与「收拢」把动物传进栏，以及牧场强加载。
 * <ul>
 *   <li>{@link Pasture} 是栅栏圈的方格；栏内 = 方格外扩 1 格（{@link Pasture#contains}），福地牧场
 *       （{@link Pasture#at}，栅栏圈在 2×2 区块的 30×30 占地周界）外扩后正好是这 4 个区块，牧场的账按区块算。在不在栏内
 *       只看实体坐标，不看栅栏。</li>
 *   <li>养殖的清点、托管、一键喂养与宰杀只看栏内（{@link #penned}）；总览页的动物数照旧数全福地。「挪生物上山」与活物
 *       标记用 {@link #penner}。</li>
 *   <li>{@link #enclose}：周界每列从真实地面（{@link LandRules#isPassable} 不算地面，花草、栅栏都跳过）上一格起叠橡木
 *       栅栏，叠到顶端比 8 邻列真实地面都高至少 1 格（邻列是流体按流体表面算）；北边正中一格的最底层放朝北的栅栏门。已
 *       是栅栏或门的格子跳过，可替换方块、树叶、原木、植物（{@link LandRules#isPlant}）换掉，栅栏顶上连着的植物（甘蔗
 *       上段）清掉，其余方块（多半是玩家的）不动、该列就此计为挡住，流体列也计为挡住。放完再按四邻重算形状，栅栏才连得
 *       上。改地形先过 {@link ModEvents#canGrief}；再圈一次只补缺的。</li>
 *   <li>{@link #unfence}：周界每列地面以上连着的栅栏与栅栏门全部拆掉（玩家放的也拆），不掉落。</li>
 *   <li>{@link #relocate}：把候选里落在旧栏内、不在新栏内的可支使动物全部传进新栏（不限头数），{@link #gather}：栏外可
 *       支使动物至多 {@value #GATHER_LIMIT} 头传进栏、冷却 {@value #GATHER_COOLDOWN_TICKS} 刻；落点都在离栅栏至少
 *       {@value #MARGIN} 格的随机地面（每头至多试 {@value #SPOT_ATTEMPTS} 次，须已加载，落点与上一格可替换或是植物）。
 *       野生蛊不是 {@link Animal}，不会被收。</li>
 *   <li>⚠ 强加载用 NeoForge 带刻工单 {@link #TICKETS}（原版强加载的区块附近没人时不随机刻），以各牧场中心为主人、各管
 *       自己的 4 区块：圈地时挂，拆除与迁移时撤旧挂新，失主时 {@link #release}，认主时 {@link #retain}；读档校验把无主
 *       福地的工单全部丢掉。</li>
 *   <li>各操作接受显式的 {@link Pasture}；GameTest 用测试区里的小方格调用，不碰别的测试。</li>
 *   <li>本文件有意超过 300 行：栏内判定、圈地与拆栅栏、迁移与收拢的落点、强加载工单都按同一个 {@link Pasture} 方格算，
 *       集中维护便于对照几何边界。</li>
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

        public static @NotNull Pasture at(int chunkX, int chunkZ) {
            return new Pasture(Siting.penMin(chunkX), Siting.penMin(chunkZ), Siting.penMax(chunkX),
                    Siting.penMax(chunkZ));
        }

        public boolean contains(double x, double z) {
            return x >= minX - 1 && x < maxX + 2 && z >= minZ - 1 && z < maxZ + 2;
        }

        public boolean contains(@NotNull Entity entity) { return contains(entity.getX(), entity.getZ()); }

        public int width() { return maxX - minX + 1; }

        public int depth() { return maxZ - minZ + 1; }

        public @NotNull BlockPos gate() { return new BlockPos((minX + maxX) / 2, 0, minZ); }

        public @NotNull BlockPos center() { return new BlockPos((minX + maxX) / 2, 0, (minZ + maxZ) / 2); }

        public @NotNull AABB box(@NotNull ServerLevel level) {
            return new AABB(minX - 1, level.getMinBuildHeight(), minZ - 1, maxX + 2, level.getMaxBuildHeight(),
                    maxZ + 2);
        }

        public @NotNull List<ChunkPos> chunks() {
            List<ChunkPos> chunks = new ArrayList<>();
            for (int x = (minX - 1) >> 4; x <= (maxX + 1) >> 4; x++) {
                for (int z = (minZ - 1) >> 4; z <= (maxZ + 1) >> 4; z++) chunks.add(new ChunkPos(x, z));
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
    public static @NotNull List<Pasture> pastures(@NotNull BlissfulLandData data) {
        List<Pasture> pastures = new ArrayList<>();
        for (ChunkPos corner : HerdState.of(data).pastures()) pastures.add(Pasture.at(corner.x, corner.z));
        return pastures;
    }

    public static @NotNull Predicate<Entity> penner(@NotNull BlissfulLandData data) {
        List<Pasture> pastures = pastures(data);
        return entity -> isPenned(pastures, entity);
    }

    public static boolean isPenned(@NotNull List<Pasture> pastures, @NotNull Entity entity) {
        for (Pasture pasture : pastures) {
            if (pasture.contains(entity)) return true;
        }
        return false;
    }

    public static @NotNull Map<ResourceLocation, List<Animal>> penned(
            @NotNull Map<ResourceLocation, List<Animal>> herds, @NotNull List<Pasture> pastures) {
        Map<ResourceLocation, List<Animal>> penned = new LinkedHashMap<>();
        herds.forEach((type, animals) -> {
            List<Animal> inside = new ArrayList<>();
            for (Animal animal : animals) {
                if (isPenned(pastures, animal)) inside.add(animal);
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

    //region Fence [圈地与拆栅栏]
    public static @Nullable Enclosed enclose(@NotNull ServerLevel level, @NotNull Pasture pasture,
                                             @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        for (ChunkPos chunk : pasture.chunks()) level.getChunk(chunk.x, chunk.z);
        BlockPos gate = pasture.gate();
        List<BlockPos> placed = new ArrayList<>();
        int blocked = 0;
        for (BlockPos column : pasture.ring()) {
            int ground = groundY(level, column.getX(), column.getZ(), false);
            if (ground == NO_GROUND) {
                blocked++;
                continue;
            }
            int top = ground + 1;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int side : new int[] {0, 1}) {
                    Direction across = side == 0 ? direction : direction.getClockWise();
                    BlockPos next = column.relative(direction).relative(across, side);
                    top = Math.max(top, groundY(level, next.getX(), next.getZ(), true) + 1);
                }
            }
            boolean isGate = column.getX() == gate.getX() && column.getZ() == gate.getZ();
            if (!stack(level, column, ground, top, isGate, placed)) blocked++;
        }
        for (BlockPos pos : placed) {
            BlockState joined = Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos);
            level.setBlock(pos, joined, Block.UPDATE_ALL);
        }
        return new Enclosed(placed.size(), blocked);
    }

    private static boolean stack(ServerLevel level, BlockPos column, int ground, int top, boolean isGate,
                                 List<BlockPos> placed) {
        for (int y = ground + 1; y <= top; y++) {
            BlockPos pos = column.atY(y);
            BlockState at = level.getBlockState(pos);
            if (isFence(at)) continue;
            if (!at.canBeReplaced() && !at.is(BlockTags.LEAVES) && !at.is(BlockTags.LOGS) && !LandRules.isPlant(at)) {
                return false;
            }
            BlockState state = isGate && y == ground + 1
                    ? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH)
                    : Blocks.OAK_FENCE.defaultBlockState();
            level.setBlock(pos, state, Block.UPDATE_ALL);
            placed.add(pos);
        }
        for (BlockPos pos = column.atY(top + 1); LandRules.isPlant(level.getBlockState(pos)); pos = pos.above()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return true;
    }

    public static @Nullable Integer unfence(@NotNull ServerLevel level, @NotNull Pasture pasture,
                                            @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        for (ChunkPos chunk : pasture.chunks()) level.getChunk(chunk.x, chunk.z);
        int removed = 0;
        for (BlockPos column : pasture.ring()) {
            int ground = groundY(level, column.getX(), column.getZ(), false);
            if (ground == NO_GROUND) continue;
            for (BlockPos pos = column.atY(ground + 1); isFence(level.getBlockState(pos)); pos = pos.above()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                removed++;
            }
        }
        return removed;
    }

    private static boolean isFence(BlockState state) {
        return state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES);
    }

    private static int groundY(ServerLevel level, int x, int z, boolean fluidSurface) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x,
                z) - 1, z);
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return fluidSurface ? pos.getY() : NO_GROUND;
            if (!LandRules.isPassable(state)) return pos.getY();
        }
        return NO_GROUND;
    }
    //endregion

    //region Move [迁移与收拢]
    public static int relocate(@NotNull ServerLevel level, @NotNull Pasture from, @NotNull Pasture to,
                               @NotNull RandomSource random, @NotNull Iterable<? extends Entity> candidates) {
        List<Animal> movers = new ArrayList<>();
        for (Entity entity : candidates) {
            if (entity instanceof Animal animal && LandRules.isManaged(animal) && from.contains(animal)
                    && !to.contains(animal)) {
                movers.add(animal);
            }
        }
        return teleport(level, to, random, movers);
    }

    public static long gatherCooldown(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return Math.max(0L, HerdState.of(data).gatherReadyAt() - level.getGameTime());
    }

    public static int gather(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull Pasture pasture,
                             @NotNull RandomSource random, @NotNull Iterable<? extends Entity> candidates) {
        List<Pasture> pastures = pastures(data);
        List<Animal> movers = new ArrayList<>();
        for (Entity entity : candidates) {
            if (movers.size() >= GATHER_LIMIT) break;
            if (entity instanceof Animal animal && LandRules.isManaged(animal) && !pasture.contains(animal)
                    && !isPenned(pastures, animal)) {
                movers.add(animal);
            }
        }
        int moved = teleport(level, pasture, random, movers);
        HerdState.of(data).setGatherReadyAt(level.getGameTime() + GATHER_COOLDOWN_TICKS);
        return moved;
    }

    private static int teleport(ServerLevel level, Pasture pasture, RandomSource random, List<Animal> movers) {
        int moved = 0;
        for (Animal animal : movers) {
            BlockPos spot = findSpot(level, pasture, random);
            if (spot == null) continue;
            animal.teleportTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            animal.setDeltaMovement(Vec3.ZERO);
            moved++;
        }
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
            int ground = groundY(level, x, z, false);
            if (ground == NO_GROUND) continue;
            BlockPos spot = new BlockPos(x, ground + 1, z);
            if (isRoom(level.getBlockState(spot)) && isRoom(level.getBlockState(spot.above()))) return spot;
        }
        return null;
    }

    private static boolean isRoom(BlockState state) { return state.canBeReplaced() || LandRules.isPlant(state); }
    //endregion

    //region Tickets [强加载]
    public static void retain(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (Pasture pasture : pastures(data)) forceChunks(level, pasture, true);
    }

    public static void release(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (Pasture pasture : pastures(data)) forceChunks(level, pasture, false);
    }

    public static void forceChunks(@NotNull ServerLevel level, @NotNull Pasture pasture, boolean add) {
        BlockPos owner = pasture.center();
        for (ChunkPos chunk : pasture.chunks()) TICKETS.forceChunk(level, owner, chunk.x, chunk.z, add, true);
    }

    private static void validateTickets(ServerLevel level, TicketHelper tickets) {
        BlissfulLand land = BlissfulLandLookup.landOf(level);
        if (land != null && BlissfulLandData.get(level.getServer(), land).hasOwner()) return;
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) tickets.removeAllTickets(owner);
    }
    //endregion
}
