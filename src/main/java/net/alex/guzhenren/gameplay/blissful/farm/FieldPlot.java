package net.alex.guzhenren.gameplay.blissful.farm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 一块标准田的占地与布局：西北角 {@code (minX, minZ)}、耕地层 {@code y}、边长 {@code size}；福地里恒为一个区块
 * （{@link #of}，边长 {@value #CHUNK}）。
 * <ul>
 *   <li>最外一圈是土径；本地坐标 {@code size / 4} 与 {@code size - 1 - size / 4} 两两相交的 4 格是水（边长 16 即
 *       (4,4)(11,4)(4,11)(11,11)），原版耕地湿润半径 4，内圈全湿；其余是耕地，作物种在耕地层上一格（{@link #cells}）。
 *       </li>
 *   <li>耕地层 {@link #layer}：各列地面里列数最多的那一层（选址已限高差 ≤ 3，至多 4 层可选）；并列取离中位数近的，
 *       再并列取低的。</li>
 *   <li>强加载工单以 {@link #center} 为主人，各田互不影响；{@link #chunk} 是西北角所在区块，田在福地里就以它记账。</li>
 *   <li>GameTest 用边长 9 的小田（同一布局：耕地 45、水 4、土径 32 格；整区块田是 192、4、60）放进测试区，不碰别的
 *       测试。</li>
 * </ul>
 */

public record FieldPlot(int minX, int y, int minZ, int size) {

    public static final int CHUNK = 16;

    public enum Cell { PATH, WATER, FARMLAND }

    public static @NotNull FieldPlot of(int chunkX, int chunkZ, int y) {
        return new FieldPlot(chunkX << 4, y, chunkZ << 4, CHUNK);
    }

    public int maxX() { return minX + size - 1; }

    public int maxZ() { return minZ + size - 1; }

    public @NotNull ChunkPos chunk() { return new ChunkPos(minX >> 4, minZ >> 4); }

    public @NotNull BlockPos center() { return new BlockPos(minX + size / 2, y, minZ + size / 2); }

    public @NotNull FieldPlot atY(int layer) { return new FieldPlot(minX, layer, minZ, size); }

    public static int layer(int @NotNull [] grounds) {
        int[] sorted = grounds.clone();
        Arrays.sort(sorted);
        int median = sorted[sorted.length / 2];
        int best = median;
        int bestCount = 0;
        int start = 0;
        while (start < sorted.length) {
            int y = sorted[start];
            int end = start;
            while (end < sorted.length && sorted[end] == y) end++;
            int count = end - start;
            if (count > bestCount || count == bestCount && Math.abs(y - median) < Math.abs(best - median)) {
                best = y;
                bestCount = count;
            }
            start = end;
        }
        return best;
    }

    public @NotNull Cell cell(int x, int z) {
        int localX = x - minX;
        int localZ = z - minZ;
        if (localX == 0 || localZ == 0 || localX == size - 1 || localZ == size - 1) return Cell.PATH;
        return isWaterLine(localX) && isWaterLine(localZ) ? Cell.WATER : Cell.FARMLAND;
    }

    private boolean isWaterLine(int local) { return local == size / 4 || local == size - 1 - size / 4; }

    public @NotNull List<BlockPos> cells() {
        List<BlockPos> cells = new ArrayList<>();
        for (int x = minX; x <= maxX(); x++) {
            for (int z = minZ; z <= maxZ(); z++) {
                if (cell(x, z) == Cell.FARMLAND) cells.add(new BlockPos(x, y + 1, z));
            }
        }
        return cells;
    }

    public @NotNull List<ChunkPos> chunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = minX >> 4; x <= maxX() >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        }
        return chunks;
    }

    public long origin() { return BlockPos.asLong(minX, y, minZ); }

    public static @NotNull FieldPlot load(long origin, int size) {
        return new FieldPlot(BlockPos.getX(origin), BlockPos.getY(origin), BlockPos.getZ(origin), size);
    }
}
