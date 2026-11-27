package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

/**
 * 把一个已加载区块扫成 {@link MapTile}：只读区块自身，不经世界查方块，区块加载事件里拿到的区块也能扫。
 * <ul>
 *   <li>{@link #tile}：不在疆域里（已割弃或原正方形外）的给整片 {@link MapTerrain#VOID}，不扫。</li>
 *   <li>{@code surface} = {@code WORLD_SURFACE} 高度图的最高非空气方块；{@code ground} 从它往下跳过
 *       {@link LandRules#isPassable}（开垦、圈地找地面同用这一条）；先碰到流体（含含水方块）就记这一格的 y 并置
 *       {@link MapTile#FLAG_FLUID}。{@code under} 是地面方块的地貌。</li>
 *   <li>{@code terrain} 取表面方块的地貌；表面是压平不画的可穿过方块（花、作物、草丛、睡莲、{@link LandRules#isThin} 的
 *       火把地毯灯笼等）时改取地面方块的，所以花画作草、作物画作耕地、睡莲画作水；雪层、树叶、原木、栅栏仍按自己。</li>
 *   <li>树冠：表面是树叶时，从上往下连着的树叶与原木算一段树冠。这段一直连到地面：最低一片树叶下面全是原木就记树干
 *       （{@link MapTile#FLAG_TRUNK}，{@code low} = 最低树叶 y），否则是贴地的灌木（{@code low} = 地面 + 1）；中间隔了
 *       空气等可穿过的方块就是悬空树冠（{@code low} = 这段的最低 y）。其余列 {@code low} = 地面 + 1。</li>
 *   <li>整列是空气（虚空）的列：两个高度都记最低建造高度减 1，地貌 {@link MapTerrain#VOID}。</li>
 *   <li>方块实体按 {@code ChunkAccess#getBlockEntitiesPos}（含待创建的）标 {@link MapTile#FLAG_BLOCK_ENTITY}，只认
 *       该列 {@code ground} 下 {@value #BLOCK_ENTITY_BELOW} 格到上 {@value #BLOCK_ENTITY_ABOVE} 格（两端含）之内的：
 *       ⚠ 即 {@link LandRules} 的整地上下格数，地下矿井里的箱子不算，客户端选址预判才与服务端对得上。</li>
 *   <li>⚠ 一片 256 列，每列从表面往下走到地面为止：寻常地形几格到几十格（树冠）；只有悬空的树叶或栅栏底下是虚空时
 *       才会走到最低建造高度，单片最坏 256 × 世界高度次取方块。调用方必须按片限预算（{@link LandMap} 每刻
 *       {@value LandMap#SCAN_BUDGET} 片）。</li>
 * </ul>
 */

public final class MapScanner {

    private MapScanner() {}

    public static final int BLOCK_ENTITY_BELOW = LandRules.FILL_DEPTH;
    public static final int BLOCK_ENTITY_ABOVE = LandRules.CLEAR_HEIGHT;

    public static @NotNull MapTile tile(@NotNull Territory territory, @NotNull LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        return territory.contains(chunkX, chunkZ) ? scan(chunk) : MapTile.empty(chunkX, chunkZ);
    }

    public static @NotNull MapTile scan(@NotNull LevelChunk chunk) {
        short[] ground = new short[MapTile.COLUMNS];
        short[] surface = new short[MapTile.COLUMNS];
        short[] low = new short[MapTile.COLUMNS];
        byte[] terrain = new byte[MapTile.COLUMNS];
        byte[] under = new byte[MapTile.COLUMNS];
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
                    low[index] = (short) (top + 1);
                    terrain[index] = MapTerrain.VOID.id();
                    under[index] = MapTerrain.VOID.id();
                    continue;
                }
                scanColumn(chunk, pos.set(minX + dx, top, minZ + dz), bottom, index, ground, low, terrain, under,
                        flags);
            }
        }
        for (BlockPos entity : chunk.getBlockEntitiesPos()) {
            int index = MapTile.index(entity.getX(), entity.getZ());
            int y = entity.getY();
            if (y >= ground[index] - BLOCK_ENTITY_BELOW && y <= ground[index] + BLOCK_ENTITY_ABOVE) {
                flags[index] |= MapTile.FLAG_BLOCK_ENTITY;
            }
        }
        return new MapTile(chunk.getPos().x, chunk.getPos().z, ground, surface, terrain, flags, low, under);
    }

    private static void scanColumn(LevelChunk chunk, BlockPos.MutableBlockPos pos, int bottom, int index,
                                   short[] ground, short[] low, byte[] terrain, byte[] under, byte[] flags) {
        int top = pos.getY();
        BlockState surface = chunk.getBlockState(pos);
        boolean canopy = surface.is(BlockTags.LEAVES);
        boolean inCanopy = canopy;
        int canopyBottom = top;
        int lowestLeaf = top;
        int floor = bottom - 1;
        BlockState floorState = null;
        for (int y = top; y >= bottom; y--) {
            BlockState state = chunk.getBlockState(pos.setY(y));
            if (!state.getFluidState().isEmpty()) {
                flags[index] |= MapTile.FLAG_FLUID;
                floor = y;
                floorState = state;
                break;
            }
            if (!LandRules.isPassable(state)) {
                floor = y;
                floorState = state;
                break;
            }
            if (!inCanopy) continue;
            if (state.is(BlockTags.LEAVES)) {
                canopyBottom = y;
                lowestLeaf = y;
            } else if (state.is(BlockTags.LOGS)) {
                canopyBottom = y;
            } else {
                inCanopy = false;
            }
        }
        ground[index] = (short) floor;
        low[index] = (short) (floor + 1);
        if (canopy && !inCanopy) {
            low[index] = (short) canopyBottom;
        } else if (canopy && floorState != null && lowestLeaf > floor + 1) {
            low[index] = (short) lowestLeaf;
            flags[index] |= MapTile.FLAG_TRUNK;
        }
        MapTerrain floorTerrain = floorState == null ? MapTerrain.VOID
                : MapTerrain.of(floorState, chunk, pos.setY(floor));
        under[index] = floorTerrain.id();
        boolean flat = floorState != null && LandRules.isPassable(surface) && !standsUp(surface)
                && !surface.is(BlockTags.SNOW);
        terrain[index] = (flat ? floorTerrain : MapTerrain.of(surface, chunk, pos.setY(top))).id();
    }

    private static boolean standsUp(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FENCES)
                || state.is(BlockTags.FENCE_GATES);
    }
}
