package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;

/**
 * 地图列的读法：光栅与拾取共用，保证画出来的和点中的是同一段方块。
 * <ul>
 *   <li>一列是 {@link MapTile} 的若干段真实方块，每段按段顶 y + 1 的平面画顶面、按段顶地貌上色；段与段之间的空气不画，
 *       没有段的列是虚空（{@link #isVoid}，不画、拾不中）。没下发的区块是迷雾：整列当一段顶在参照迷雾平面下一格、往下
 *       实心的段。</li>
 *   <li>{@link #exposed}：某段朝一个邻列露出的崖面 = 本段减去邻列各段（迷雾邻列盖住迷雾平面以下），可能断成几截，
 *       按「段顶下第几格」写成左闭右开的区间。往下实心的段（{@link MapTile#OPEN}）朝没有实心底的邻列（虚空、整列悬空）
 *       只垂 {@value #VOID_SKIRT} 格裙边。</li>
 *   <li>{@link #wet}：地面那一格落在水段里，离岸距离与水色按它算（悬在水面上的树冠下面仍是水）。</li>
 *   <li>⚠ {@link #exposed} 的输出数组至少 {@value #EXPOSED_LENGTH} 格（邻列 {@value MapTile#MAX_RUNS} 段至多切出
 *       {@value MapTile#MAX_RUNS} + 1 截）。</li>
 * </ul>
 */

public final class MapColumns {

    public static final int VOID_SKIRT = 8;
    public static final int EXPOSED_LENGTH = 2 * (MapTile.MAX_RUNS + 1);
    private static final int DOWNWARD = Integer.MIN_VALUE / 4;

    private MapColumns() {}

    public static boolean isVoid(MapTile tile, int index) { return tile.runs(index) == 0; }

    public static MapTerrain terrain(MapTile tile, int index) {
        return isVoid(tile, index) ? MapTerrain.VOID : tile.runTerrain(tile.run(index, 0));
    }

    public static int top(MapTile tile, int index) { return tile.runTop(tile.run(index, 0)); }

    public static int bottom(MapTile tile, int run) { return tile.isOpen(run) ? DOWNWARD : tile.runBottom(run); }

    public static boolean wet(MapTile tile, int index) {
        int ground = tile.ground()[index];
        for (int k = 0; k < tile.runs(index); k++) {
            int run = tile.run(index, k);
            if (tile.runTop(run) >= ground && bottom(tile, run) <= ground) {
                return tile.runTerrain(run) == MapTerrain.WATER;
            }
        }
        return false;
    }

    public static int exposed(TileSource tiles, int fogPlane, int x, int z, int top, int bottom, int[] out) {
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        int index = MapTile.index(x, z);
        boolean grounded = tile == null || !isVoid(tile, index) && tile.isOpen(tile.run(index, tile.runs(index) - 1));
        int low = bottom != MapTile.OPEN ? bottom : grounded ? DOWNWARD : top - VOID_SKIRT + 1;
        int cursor = top;
        int count = 0;
        int runs = tile == null ? 1 : tile.runs(index);
        for (int k = 0; k < runs && cursor >= low; k++) {
            int coverTop = tile == null ? fogPlane - 1 : tile.runTop(tile.run(index, k));
            int coverBottom = tile == null ? DOWNWARD : bottom(tile, tile.run(index, k));
            if (coverBottom > cursor) continue;
            if (coverTop < cursor) count = emit(out, count, top, cursor, Math.max(low, coverTop + 1));
            cursor = Math.min(cursor, coverBottom - 1);
        }
        if (cursor >= low) count = emit(out, count, top, cursor, low);
        return count;
    }

    private static int emit(int[] out, int count, int top, int high, int low) {
        if (high < low) return count;
        out[2 * count] = top - high;
        out[2 * count + 1] = top - low + 1;
        return count + 1;
    }
}
