package net.alex.guzhenren.client.screen.blissful.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;
import net.alex.guzhenren.gameplay.blissful.map.MapTile;

/**
 * 俯视等高线视角的光栅：每列填一格顶面色（按左上两邻的地面高差做三阶明暗，迎光面带 {@link MapPalette} 颗粒，水面按
 * 离岸距离取深浅），再沿列边画等高线，并挑出加粗线的数字标注位置。
 * <ul>
 *   <li>等高线按真实地面高 {@code ground}：相邻两列跨过 {@value #MINOR_STEP} 的倍数就在较高一列贴边画 1 像素暗线；跨过
 *       {@value #MAJOR_STEP} 的倍数画 2 像素加粗线，一次跨多条时取最高的加粗级，否则取最高的细级
 *       （{@link #level}）。</li>
 *   <li>流体列、迷雾与虚空两侧不画线；标注每个棋盘黑格区块至多一处（离区块中心最近的加粗线格），全图至多
 *       {@value #MAX_LABELS} 处，坐标是全局地图像素（格中心）。</li>
 * </ul>
 */

final class MapContours {

    static final int MINOR_STEP = 8;
    static final int MAJOR_STEP = 40;
    static final int NONE = Integer.MIN_VALUE;
    static final float MINOR_SHADE = 0.70F;
    static final float MAJOR_SHADE = 0.48F;
    private static final int MAX_LABELS = 64;

    private MapContours() {}

    static int level(int first, int second) {
        int high = Math.max(first, second);
        int low = Math.min(first, second);
        if (high == low) return NONE;
        int major = Math.floorDiv(high, MAJOR_STEP) * MAJOR_STEP;
        if (major > low) return major;
        int minor = Math.floorDiv(high, MINOR_STEP) * MINOR_STEP;
        return minor > low ? minor : NONE;
    }

    static List<MapRaster.Label> draw(MapRaster.Canvas canvas, MapProjection projection, MapGrid grid) {
        int cell = projection.cell();
        int uMin = Math.floorDiv(canvas.originX, cell);
        int uMax = Math.floorDiv(canvas.originX + canvas.width - 1, cell);
        int vMin = Math.floorDiv(canvas.originY, cell);
        int vMax = Math.floorDiv(canvas.originY + canvas.height - 1, cell);
        for (int v = vMin; v <= vMax; v++) {
            for (int u = uMin; u <= uMax; u++) fill(canvas, projection, grid, u, v);
        }
        Map<Long, int[]> best = new HashMap<>();
        for (int v = vMin; v <= vMax; v++) {
            for (int u = uMin; u <= uMax; u++) {
                line(canvas, projection, grid, best, u, v, true);
                line(canvas, projection, grid, best, u, v, false);
            }
        }
        List<MapRaster.Label> labels = new ArrayList<>();
        for (int[] label : best.values()) {
            if (labels.size() >= MAX_LABELS) break;
            labels.add(new MapRaster.Label(label[1], label[2], label[3]));
        }
        return labels;
    }

    //region Fill [填色]
    private static void fill(MapRaster.Canvas canvas, MapProjection projection, MapGrid grid, int u, int v) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = grid.tile(x >> 4, z >> 4);
        int colour;
        float fade = 1.0F;
        if (tile == null) {
            colour = ((x >> 4) + (z >> 4) & 1) == 0 ? MapPalette.FOG : MapPalette.FOG_ALT;
        } else {
            int index = MapTile.index(x, z);
            if (MapColumns.isVoid(tile, index)) return;
            colour = shade(projection, grid, tile, index, u, v);
            fade = grid.fade(x >> 4, z >> 4, x & 15, z & 15);
        }
        int cell = projection.cell();
        for (int row = 0; row < cell; row++) {
            canvas.span(u * cell, u * cell + cell, v * cell + row, colour, MapPalette.FOG, fade);
        }
    }

    private static int shade(MapProjection projection, MapGrid grid, MapTile tile, int index, int u, int v) {
        MapTerrain terrain = MapColumns.terrain(tile, index);
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        if (terrain == MapTerrain.WATER) return MapPalette.water(MapRaster.shoreDistance(grid, x, z));
        if (terrain == MapTerrain.LAVA) return MapPalette.top(terrain);
        int ground = tile.ground()[index];
        int delta = 0;
        int west = ground(projection, grid, u - 1, v);
        int north = ground(projection, grid, u, v - 1);
        if (west != NONE) delta += ground - west;
        if (north != NONE) delta += ground - north;
        if (delta >= 0) return MapPalette.top(terrain, x, z);
        return delta >= -2 ? MapPalette.left(terrain) : MapPalette.right(terrain);
    }

    private static int ground(MapProjection projection, MapGrid grid, int u, int v) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = grid.tile(x >> 4, z >> 4);
        if (tile == null) return NONE;
        int index = MapTile.index(x, z);
        if (MapColumns.isVoid(tile, index) || (tile.flags()[index] & MapTile.FLAG_FLUID) != 0) return NONE;
        return tile.ground()[index];
    }
    //endregion

    //region Lines [等高线]
    private static void line(MapRaster.Canvas canvas, MapProjection projection, MapGrid grid, Map<Long, int[]> best,
                             int u, int v, boolean across) {
        int nextU = across ? u + 1 : u;
        int nextV = across ? v : v + 1;
        int here = ground(projection, grid, u, v);
        int there = ground(projection, grid, nextU, nextV);
        if (here == NONE || there == NONE) return;
        int level = level(here, there);
        if (level == NONE) return;

        boolean major = level % MAJOR_STEP == 0;
        float shade = major ? MAJOR_SHADE : MINOR_SHADE;
        boolean hereHigh = here > there;
        int cell = projection.cell();
        int boundary = across ? nextU * cell : nextV * cell;
        int step = hereHigh ? -1 : 1;
        int first = hereHigh ? boundary - 1 : boundary;
        for (int thickness = 0; thickness < (major ? 2 : 1); thickness++) {
            int at = first + step * thickness;
            for (int i = 0; i < cell; i++) {
                if (across) canvas.darken(at, v * cell + i, shade);
                else canvas.darken(u * cell + i, at, shade);
            }
        }
        if (major) offer(canvas, projection, best, hereHigh ? u : nextU, hereHigh ? v : nextV, level);
    }

    private static void offer(MapRaster.Canvas canvas, MapProjection projection, Map<Long, int[]> best, int u, int v,
                              int level) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        if (((x >> 4) + (z >> 4) & 1) != 0) return;
        int cell = projection.cell();
        int labelX = u * cell + cell / 2;
        int labelY = v * cell + cell / 2;
        if (!canvas.contains(labelX, labelY)) return;
        int distance = square(2 * (x & 15) - 15) + square(2 * (z & 15) - 15);
        long chunk = (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
        int[] current = best.get(chunk);
        if (current == null || distance < current[0]) best.put(chunk, new int[] { distance, labelX, labelY, level });
    }

    private static int square(int value) { return value * value; }
    //endregion
}
