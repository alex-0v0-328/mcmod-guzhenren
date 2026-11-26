package net.alex.guzhenren.gameplay.blissful.map;

import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.blissful.land.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

/**
 * 把一个已加载区块扫成 {@link MapTile}：只读区块自身，不经世界查方块，区块加载事件里拿到的区块也能扫。
 * <ul>
 *   <li>{@link #tile}：不在疆域里（已割弃或原正方形外）的给整片 {@link MapTerrain#VOID}，不扫。</li>
 *   <li>{@code surface} = {@code WORLD_SURFACE} 高度图的最高非空气方块，{@code terrain} 取它的地貌；{@code ground} 从
 *       它往下跳过 {@link #isPassable}（空气、树叶、原木、可替换方块、植物、栅栏与栅栏门），{@code FarmWork} 与
 *       {@code PastureWork} 找地面共用这一条；先碰到流体（含含水方块）就记这一格的 y 并置
 *       {@link MapTile#FLAG_FLUID}。</li>
 *   <li>植物（{@link #isPlant}）= 原版 {@link BushBlock}（花、草丛、树苗、作物、蘑菇、浆果丛、睡莲等）与甘蔗：不算地
 *       面，圈地时被栅栏替换；仙人掌、竹子有碰撞箱，仍算地面。</li>
 *   <li>整列是空气（虚空）的列：两个高度都记最低建造高度减 1，地貌 {@link MapTerrain#VOID}。</li>
 *   <li>方块实体按 {@code ChunkAccess#getBlockEntitiesPos}（含待创建的）标 {@link MapTile#FLAG_BLOCK_ENTITY}，只认
 *       该列 {@code ground} 下 {@value #BLOCK_ENTITY_BELOW} 格到上 {@value #BLOCK_ENTITY_ABOVE} 格（两端含）之内的：
 *       ⚠ 与 {@link FarmWork} 开垦的填土深度与清整高度一致，地下矿井里的箱子不算，客户端选址预判才与服务端对得上。</li>
 *   <li>⚠ 一片 256 列，每列从表面往下走到地面为止：寻常地形几格到几十格（树冠）；只有悬空的树叶或栅栏底下是虚空时
 *       才会走到最低建造高度，单片最坏 256 × 世界高度次取方块。调用方必须按片限预算（{@link LandMap} 每刻
 *       {@value LandMap#SCAN_BUDGET} 片）。</li>
 * </ul>
 */

public final class MapScanner {

    private MapScanner() {}

    public static final int BLOCK_ENTITY_BELOW = FarmWork.FILL_DEPTH;
    public static final int BLOCK_ENTITY_ABOVE = FarmWork.CLEAR_HEIGHT;

    public static @NotNull MapTile tile(@NotNull Territory territory, @NotNull LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        return territory.contains(chunkX, chunkZ) ? scan(chunk) : MapTile.empty(chunkX, chunkZ);
    }

    public static @NotNull MapTile scan(@NotNull LevelChunk chunk) {
        short[] ground = new short[MapTile.COLUMNS];
        short[] surface = new short[MapTile.COLUMNS];
        byte[] terrain = new byte[MapTile.COLUMNS];
        byte[] flags = new byte[MapTile.COLUMNS];
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int bottom = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int index = MapTile.index(dx, dz);
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
                surface[index] = (short) top;
                if (top < bottom) {
                    ground[index] = (short) top;
                    terrain[index] = MapTerrain.VOID.id();
                    continue;
                }
                pos.set(minX + dx, top, minZ + dz);
                terrain[index] = MapTerrain.of(chunk.getBlockState(pos), chunk, pos).id();
                int floor = bottom - 1;
                for (int y = top; y >= bottom; y--) {
                    BlockState state = chunk.getBlockState(pos.setY(y));
                    if (!state.getFluidState().isEmpty()) {
                        flags[index] |= MapTile.FLAG_FLUID;
                        floor = y;
                        break;
                    }
                    if (!isPassable(state)) {
                        floor = y;
                        break;
                    }
                }
                ground[index] = (short) floor;
            }
        }
        for (BlockPos entity : chunk.getBlockEntitiesPos()) {
            int index = MapTile.index(entity.getX(), entity.getZ());
            int y = entity.getY();
            if (y >= ground[index] - BLOCK_ENTITY_BELOW && y <= ground[index] + BLOCK_ENTITY_ABOVE) {
                flags[index] |= MapTile.FLAG_BLOCK_ENTITY;
            }
        }
        return new MapTile(chunk.getPos().x, chunk.getPos().z, ground, surface, terrain, flags);
    }

    public static boolean isPassable(@NotNull BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()
                || isPlant(state) || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES);
    }

    public static boolean isPlant(@NotNull BlockState state) {
        return state.getBlock() instanceof BushBlock || state.is(Blocks.SUGAR_CANE);
    }
}
