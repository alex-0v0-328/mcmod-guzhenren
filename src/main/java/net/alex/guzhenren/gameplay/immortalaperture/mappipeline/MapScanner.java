package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Arrays;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

/**
 * 把一个已加载区块扫成 {@link MapTile}：只读区块自身的方块，邻区块只读已加载的高度图，区块加载事件里拿到的区块也能扫。
 * <ul>
 *   <li>{@link #tile}：不在疆域里（已割弃或原正方形外）的给整片 {@link MapTerrain#VOID}，不扫。</li>
 *   <li>段：每列从 {@code WORLD_SURFACE} 高度图的最高方块往下，连着的同类方块记一段（土石等地面、树叶、原木、栅栏与
 *       栅栏门、水、岩浆各一类，含水方块按水），空气不记。花、作物、草丛、火把、地毯等 {@link LandRules#isPassable} 的
 *       小件压平不成段，所以花画作草、作物画作耕地；紧贴段顶的雪层把这段顶面改成雪色。地面段的地貌按段顶方块
 *       {@link MapTerrain#of}（按方块状态每片归类一次，不看位置）。</li>
 *   <li>扫描深度：每列取它与前后左右四列里最低的地表（{@code MOTION_BLOCKING_NO_LEAVES} 高度图，不算树叶；邻区块没加载
 *       或整列空气的不算）再往下 {@value #DEPTH_BELOW} 格为扫描底，扫到底以下第一块地面类方块就停，这段记
 *       {@link MapTile#OPEN}（往下当实心）。崖面只朝四邻露出，所以悬空物下的空隙、崖壁上的洞口都如实记下；流体、树叶
 *       不停，一直扫到下面的地面；扫到最低建造高度的段是实底。</li>
 *   <li>{@code ground} 从上往下跳过 {@link LandRules#isPassable}（开垦、圈地找地面同用这一条），先碰到流体（含含水方块）
 *       就记这一格并置 {@link MapTile#FLAG_FLUID}；整列都可穿过的记最低建造高度减 1。</li>
 *   <li>方块实体按 {@code ChunkAccess#getBlockEntitiesPos}（含待创建的）标 {@link MapTile#FLAG_BLOCK_ENTITY}，只认
 *       该列 {@code ground} 下 {@value #BLOCK_ENTITY_BELOW} 格到上 {@value #BLOCK_ENTITY_ABOVE} 格（两端含）之内的：
 *       ⚠ 即 {@link LandRules} 的整地上下格数，地下矿井里的箱子不算，客户端选址预判才与服务端对得上。</li>
 *   <li>⚠ 每列读到扫描底为止：寻常地形每列十来次取方块（直接读区块段，方块归类每片缓存）；崖顶要读到崖底，悬空物
 *       下要读穿空隙，单片最坏 256 × 世界高度次。调用方必须按片限预算（{@link LandMap} 每刻
 *       {@value LandMap#SCAN_BUDGET} 片）；单列段数到 {@value MapTile#MAX_RUNS} 即把最后一段记成往下实心并停。</li>
 * </ul>
 */

public final class MapScanner {

    private MapScanner() {}

    public static final int BLOCK_ENTITY_BELOW = LandRules.FILL_DEPTH;
    public static final int BLOCK_ENTITY_ABOVE = LandRules.CLEAR_HEIGHT;
    public static final int DEPTH_BELOW = 8;
    private static final int FRAME = 18;
    private static final int AIR = 0;
    private static final int THIN = 1;
    private static final int SNOW_LAYER = 2;
    private static final int GROUND = 3;
    private static final int FOLIAGE = 4;
    private static final int WOOD = 5;
    private static final int FENCE = 6;
    private static final int WATER = 7;
    private static final int LAVA = 8;
    private static final int KIND = 15;
    private static final int FLUID = 16;
    private static final int PASSABLE = 32;
    private static final int TERRAIN_SHIFT = 8;
    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();

    public static @NotNull MapTile tile(@NotNull Territory territory, @NotNull LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        return territory.contains(chunkX, chunkZ) ? scan(chunk) : MapTile.empty(chunkX, chunkZ);
    }

    public static @NotNull MapTile scan(@NotNull LevelChunk chunk) {
        MapTile.Builder builder = new MapTile.Builder();
        Kinds kinds = new Kinds();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int bottom = chunk.getMinBuildHeight();
        int[] floors = floors(chunk);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int index = MapTile.index(dx, dz);
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
                if (top < bottom) {
                    builder.ground(index, top);
                    continue;
                }
                pos.set(minX + dx, top, minZ + dz);
                scanColumn(chunk, kinds, builder, index, pos, floors[index]);
            }
        }
        for (BlockPos entity : chunk.getBlockEntitiesPos()) {
            int index = MapTile.index(entity.getX(), entity.getZ());
            int ground = builder.groundOf(index);
            int y = entity.getY();
            if (y >= ground - BLOCK_ENTITY_BELOW && y <= ground + BLOCK_ENTITY_ABOVE) {
                builder.flag(index, MapTile.FLAG_BLOCK_ENTITY);
            }
        }
        return builder.build(chunk.getPos().x, chunk.getPos().z);
    }

    //region Column [列]
    private static void scanColumn(LevelChunk chunk, Kinds kinds, MapTile.Builder builder, int index,
                                   BlockPos.MutableBlockPos pos, int floor) {
        int bottom = chunk.getMinBuildHeight();
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        LevelChunkSection section = null;
        boolean groundFound = false;
        boolean snowAbove = false;
        int runTop = Integer.MIN_VALUE;
        int runKind = AIR;
        MapTerrain runTerrain = MapTerrain.VOID;
        for (int y = pos.getY(); y >= bottom; y--) {
            if (section == null || (y & 15) == 15) section = chunk.getSection(chunk.getSectionIndex(y));
            BlockState state = section.hasOnlyAir() ? AIR_STATE : section.getBlockState(localX, y & 15, localZ);
            int info = kinds.of(state, chunk, pos, y);
            int kind = info & KIND;
            if (!groundFound && ((info & FLUID) != 0 || (info & PASSABLE) == 0)) {
                groundFound = true;
                builder.ground(index, y);
                if ((info & FLUID) != 0) builder.flag(index, MapTile.FLAG_FLUID);
            }
            if (kind <= SNOW_LAYER) {
                if (runTop != Integer.MIN_VALUE) builder.run(index, runTop, y + 1, runTerrain);
                runTop = Integer.MIN_VALUE;
                snowAbove = kind == SNOW_LAYER;
                continue;
            }
            if (runTop == Integer.MIN_VALUE || kind != runKind) {
                if (runTop != Integer.MIN_VALUE) builder.run(index, runTop, y + 1, runTerrain);
                runTop = y;
                runKind = kind;
                runTerrain = snowAbove && kind != WATER && kind != LAVA ? MapTerrain.SNOW
                        : MapTerrain.byId(info >>> TERRAIN_SHIFT);
                if (builder.runs(index) == MapTile.MAX_RUNS - 1) {
                    builder.run(index, runTop, MapTile.OPEN, runTerrain);
                    return;
                }
            }
            snowAbove = false;
            if (kind == GROUND && y <= floor) {
                builder.run(index, runTop, MapTile.OPEN, runTerrain);
                return;
            }
        }
        if (runTop != Integer.MIN_VALUE) builder.run(index, runTop, bottom, runTerrain);
        if (!groundFound) builder.ground(index, bottom - 1);
    }

    private static int classify(BlockState state, LevelChunk chunk, BlockPos pos) {
        if (state.isAir()) return AIR | PASSABLE;
        boolean fluid = !state.getFluidState().isEmpty();
        boolean passable = LandRules.isPassable(state);
        int kind;
        if (state.is(BlockTags.LEAVES)) {
            kind = FOLIAGE;
        } else if (state.is(BlockTags.LOGS)) {
            kind = WOOD;
        } else if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES)) {
            kind = FENCE;
        } else if (fluid) {
            kind = state.getFluidState().is(FluidTags.LAVA) ? LAVA : WATER;
        } else if (passable) {
            kind = state.is(BlockTags.SNOW) ? SNOW_LAYER : THIN;
        } else {
            kind = GROUND;
        }
        MapTerrain terrain = switch (kind) {
            case FOLIAGE -> MapTerrain.FOLIAGE;
            case WOOD -> MapTerrain.WOOD;
            case FENCE -> MapTerrain.FENCE;
            case WATER -> MapTerrain.WATER;
            case LAVA -> MapTerrain.LAVA;
            case GROUND -> MapTerrain.of(state, chunk, pos);
            default -> MapTerrain.VOID;
        };
        return kind | (fluid ? FLUID : 0) | (passable ? PASSABLE : 0) | terrain.id() << TERRAIN_SHIFT;
    }
    //endregion

    //region Floor [扫描底]
    static int[] floors(@NotNull LevelChunk chunk) {
        int[] tops = new int[FRAME * FRAME];
        Arrays.fill(tops, Integer.MAX_VALUE);
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) tops[(dz + 1) * FRAME + dx + 1] = surface(chunk, dx, dz);
        }
        Level level = chunk.getLevel();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        ChunkAccess west = level.getChunkSource().getChunkNow(chunkX - 1, chunkZ);
        ChunkAccess east = level.getChunkSource().getChunkNow(chunkX + 1, chunkZ);
        ChunkAccess north = level.getChunkSource().getChunkNow(chunkX, chunkZ - 1);
        ChunkAccess south = level.getChunkSource().getChunkNow(chunkX, chunkZ + 1);
        for (int i = 0; i < 16; i++) {
            if (west != null) tops[(i + 1) * FRAME] = surface(west, 15, i);
            if (east != null) tops[(i + 1) * FRAME + 17] = surface(east, 0, i);
            if (north != null) tops[i + 1] = surface(north, i, 15);
            if (south != null) tops[17 * FRAME + i + 1] = surface(south, i, 0);
        }
        int[] floors = new int[MapTile.COLUMNS];
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int at = (dz + 1) * FRAME + dx + 1;
                int lowest = Math.min(Math.min(tops[at], tops[at - 1]),
                        Math.min(Math.min(tops[at + 1], tops[at - FRAME]), tops[at + FRAME]));
                floors[MapTile.index(dx, dz)] = lowest == Integer.MAX_VALUE ? chunk.getMinBuildHeight()
                        : lowest - DEPTH_BELOW;
            }
        }
        return floors;
    }

    private static int surface(ChunkAccess chunk, int localX, int localZ) {
        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
        return top < chunk.getMinBuildHeight() ? Integer.MAX_VALUE : top;
    }
    //endregion

    /** 一片之内的方块归类与地貌缓存：连着的同一方块状态直接复用上一格的结果，其余查表，每种状态只归类一次。 */
    private static final class Kinds {

        private final Reference2IntOpenHashMap<BlockState> known = new Reference2IntOpenHashMap<>(64);
        private BlockState last;
        private int lastInfo;

        Kinds() { known.defaultReturnValue(-1); }

        int of(BlockState state, LevelChunk chunk, BlockPos.MutableBlockPos pos, int y) {
            if (state == last) return lastInfo;
            int info = known.getInt(state);
            if (info < 0) {
                info = classify(state, chunk, pos.setY(y));
                known.put(state, info);
            }
            last = state;
            lastInfo = info;
            return info;
        }
    }
}
