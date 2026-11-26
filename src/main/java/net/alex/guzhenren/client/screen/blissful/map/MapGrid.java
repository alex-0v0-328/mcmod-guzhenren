package net.alex.guzhenren.client.screen.blissful.map;

import net.alex.guzhenren.gameplay.blissful.map.MapTile;
import net.alex.guzhenren.gameplay.blissful.map.MapView;
import org.jetbrains.annotations.Nullable;

/**
 * 地图片快照[Map Grid]：渲染线程按区块矩形把 {@link TileSource} 里的地图片引用拷进数组，交给后台光栅线程读；地图片本身
 * 不可变，只拷引用。
 * <ul>
 *   <li>⚠ {@link #capture} 只拷引用（渲染线程上不到 1 毫秒）；高度范围与迷雾掩码在第一次读 {@link #minPlane}、
 *       {@link #maxPlane}、{@link #fade} 时才算，应由光栅线程触发；同一快照不要跨线程同时读。</li>
 *   <li>矩形每边至多 {@value #MAX_SIDE} 区块，多出的从高端截掉；矩形外一律当迷雾。</li>
 *   <li>{@link #minPlane}/{@link #maxPlane}：快照内非虚空列顶面平面的范围，光栅据此裁扫描行；没有任何陆地列时
 *       {@link #hasLand} 为假、两者都是 {@code 0}。</li>
 *   <li>{@link #fade}：迷雾渐隐系数（0 = 贴着迷雾，1 = 离迷雾 ≥ {@value #FADE_BLOCKS} 格），按本区块八个邻区块里没
 *       下发的那几个算列中心到它们的最近距离；虚空区块算已知，不渐隐。</li>
 * </ul>
 */

public final class MapGrid implements TileSource {

    public static final int MAX_SIDE = 96;
    public static final int FADE_BLOCKS = 6;
    private static final int WEST = 1;
    private static final int EAST = 2;
    private static final int NORTH = 4;
    private static final int SOUTH = 8;
    private static final int NORTH_WEST = 16;
    private static final int NORTH_EAST = 32;
    private static final int SOUTH_WEST = 64;
    private static final int SOUTH_EAST = 128;
    private final int minChunkX;
    private final int minChunkZ;
    private final int width;
    private final int depth;
    private final MapTile[] tiles;
    private byte @Nullable [] fogMasks;
    private int minPlane;
    private int maxPlane;
    private boolean land;

    private MapGrid(int minChunkX, int minChunkZ, int width, int depth, MapTile[] tiles) {
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.width = width;
        this.depth = depth;
        this.tiles = tiles;
    }

    private byte[] indexed() {
        if (fogMasks != null) return fogMasks;
        byte[] masks = new byte[tiles.length];
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int i = 0; i < tiles.length; i++) {
            MapTile tile = tiles[i];
            if (tile == null) continue;
            masks[i] = (byte) mask(minChunkX + i % width, minChunkZ + i / width);
            for (int index = 0; index < MapTile.COLUMNS; index++) {
                if (MapColumns.isVoid(tile, index)) continue;
                int plane = MapColumns.top(tile, index) + 1;
                low = Math.min(low, plane);
                high = Math.max(high, plane);
            }
        }
        land = low <= high;
        minPlane = land ? low : 0;
        maxPlane = land ? high : 0;
        fogMasks = masks;
        return masks;
    }

    public static MapGrid capture(TileSource source, MapView area) {
        if (area.isEmpty()) return new MapGrid(0, 0, 0, 0, new MapTile[0]);
        int width = Math.min(area.width(), MAX_SIDE);
        int depth = Math.min(area.depth(), MAX_SIDE);
        MapTile[] tiles = new MapTile[width * depth];
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                tiles[dz * width + dx] = source.tile(area.minChunkX() + dx, area.minChunkZ() + dz);
            }
        }
        return new MapGrid(area.minChunkX(), area.minChunkZ(), width, depth, tiles);
    }

    @Override
    public @Nullable MapTile tile(int chunkX, int chunkZ) {
        int dx = chunkX - minChunkX;
        int dz = chunkZ - minChunkZ;
        if (dx < 0 || dz < 0 || dx >= width || dz >= depth) return null;
        return tiles[dz * width + dx];
    }

    public int minPlane() {
        indexed();
        return minPlane;
    }

    public int maxPlane() {
        indexed();
        return maxPlane;
    }

    public boolean hasLand() {
        indexed();
        return land;
    }

    //region Fog fade [迷雾渐隐]
    public float fade(int chunkX, int chunkZ, int localX, int localZ) {
        int dx = chunkX - minChunkX;
        int dz = chunkZ - minChunkZ;
        if (dx < 0 || dz < 0 || dx >= width || dz >= depth) return 0.0F;
        return fade(indexed()[dz * width + dx] & 0xFF, localX, localZ);
    }

    static float fade(int mask, int localX, int localZ) {
        if (mask == 0) return 1.0F;
        double west = localX + 0.5;
        double east = 15.5 - localX;
        double north = localZ + 0.5;
        double south = 15.5 - localZ;
        double nearest = Double.POSITIVE_INFINITY;
        if ((mask & WEST) != 0) nearest = Math.min(nearest, west);
        if ((mask & EAST) != 0) nearest = Math.min(nearest, east);
        if ((mask & NORTH) != 0) nearest = Math.min(nearest, north);
        if ((mask & SOUTH) != 0) nearest = Math.min(nearest, south);
        if ((mask & NORTH_WEST) != 0) nearest = Math.min(nearest, Math.hypot(west, north));
        if ((mask & NORTH_EAST) != 0) nearest = Math.min(nearest, Math.hypot(east, north));
        if ((mask & SOUTH_WEST) != 0) nearest = Math.min(nearest, Math.hypot(west, south));
        if ((mask & SOUTH_EAST) != 0) nearest = Math.min(nearest, Math.hypot(east, south));
        return (float) Math.min(1.0, nearest / FADE_BLOCKS);
    }

    private int mask(int chunkX, int chunkZ) {
        int mask = 0;
        if (tile(chunkX - 1, chunkZ) == null) mask |= WEST;
        if (tile(chunkX + 1, chunkZ) == null) mask |= EAST;
        if (tile(chunkX, chunkZ - 1) == null) mask |= NORTH;
        if (tile(chunkX, chunkZ + 1) == null) mask |= SOUTH;
        if (tile(chunkX - 1, chunkZ - 1) == null) mask |= NORTH_WEST;
        if (tile(chunkX + 1, chunkZ - 1) == null) mask |= NORTH_EAST;
        if (tile(chunkX - 1, chunkZ + 1) == null) mask |= SOUTH_WEST;
        if (tile(chunkX + 1, chunkZ + 1) == null) mask |= SOUTH_EAST;
        return mask;
    }

    static int maskOf(boolean west, boolean east, boolean north, boolean south) {
        return (west ? WEST : 0) | (east ? EAST : 0) | (north ? NORTH : 0) | (south ? SOUTH : 0);
    }
    //endregion
}
