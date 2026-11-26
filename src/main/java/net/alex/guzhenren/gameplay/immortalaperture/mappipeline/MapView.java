package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 地图视口[Map View]：客户端订阅的区块矩形（两端都含），服务端按它扫描、探索与挑标记，两端共用同一套规则。
 * <ul>
 *   <li>{@link #isEmpty}：任一轴 {@code min > max}，即退订；{@link #isValid}：非空且每边至多 {@value #MAX_SIDE} 区块，
 *       越界的整包忽略。</li>
 *   <li>{@link #byDistance}：视口内全部区块按离中心近到远排（同距先 z 后 x），扫描与探索都按这个顺序；
 *       {@link #distanceSq} 按半区块计（中心可能落在区块边上），{@link #blockDistanceSq} 按方块计。</li>
 *   <li>{@link #keeps}：视口四周外扩 {@value #KEEP_MARGIN} 区块的范围；换视口时客户端丢掉范围外的地图片、服务端同时
 *       忘掉「已发过」的记录，两端按同一个视口裁，下次回来照常重发。</li>
 *   <li>{@link #STREAM_CODEC}：两角四个 varint，不校验（服务端收到后按 {@link #isEmpty}、{@link #isValid} 判）。</li>
 * </ul>
 */

public record MapView(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {

    public static final int MAX_SIDE = 64;
    public static final int KEEP_MARGIN = 32;
    public static final MapView NONE = new MapView(0, 0, -1, -1);
    public static final StreamCodec<ByteBuf, MapView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapView::minChunkX,
            ByteBufCodecs.VAR_INT, MapView::minChunkZ,
            ByteBufCodecs.VAR_INT, MapView::maxChunkX,
            ByteBufCodecs.VAR_INT, MapView::maxChunkZ,
            MapView::new);

    public boolean isEmpty() { return minChunkX > maxChunkX || minChunkZ > maxChunkZ; }

    public boolean isValid() {
        return !isEmpty() && (long) maxChunkX - minChunkX < MAX_SIDE && (long) maxChunkZ - minChunkZ < MAX_SIDE;
    }

    public int width() { return isEmpty() ? 0 : maxChunkX - minChunkX + 1; }

    public int depth() { return isEmpty() ? 0 : maxChunkZ - minChunkZ + 1; }

    public boolean contains(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    public boolean contains(long chunk) { return contains(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)); }

    public boolean keeps(int chunkX, int chunkZ) {
        return !isEmpty() && chunkX >= minChunkX - KEEP_MARGIN && chunkX <= maxChunkX + KEEP_MARGIN
                && chunkZ >= minChunkZ - KEEP_MARGIN && chunkZ <= maxChunkZ + KEEP_MARGIN;
    }

    public boolean keeps(long chunk) { return keeps(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)); }

    public long distanceSq(int chunkX, int chunkZ) {
        long dx = 2L * chunkX - minChunkX - maxChunkX;
        long dz = 2L * chunkZ - minChunkZ - maxChunkZ;
        return dx * dx + dz * dz;
    }

    public double blockDistanceSq(double x, double z) {
        double dx = x - (minChunkX + maxChunkX + 1) * 8.0D;
        double dz = z - (minChunkZ + maxChunkZ + 1) * 8.0D;
        return dx * dx + dz * dz;
    }

    public long @NotNull [] byDistance() {
        if (isEmpty()) return new long[0];
        int width = width();
        Long[] chunks = new Long[width * depth()];
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = ChunkPos.asLong(minChunkX + i % width, minChunkZ + i / width);
        }
        Arrays.sort(chunks, (a, b) -> {
            int byDistance = Long.compare(distanceSq(ChunkPos.getX(a), ChunkPos.getZ(a)),
                    distanceSq(ChunkPos.getX(b), ChunkPos.getZ(b)));
            if (byDistance != 0) return byDistance;
            int byZ = Integer.compare(ChunkPos.getZ(a), ChunkPos.getZ(b));
            return byZ != 0 ? byZ : Integer.compare(ChunkPos.getX(a), ChunkPos.getX(b));
        });
        long[] ordered = new long[chunks.length];
        for (int i = 0; i < chunks.length; i++) ordered[i] = chunks[i];
        return ordered;
    }
}
