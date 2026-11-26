package net.alex.guzhenren.client.screen.blissful.map;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;
import net.alex.guzhenren.gameplay.blissful.map.MapTile;

/**
 * 地图光栅[Map Raster]：把 {@link MapGrid} 快照画成一块 ARGB 像素（纯函数，不碰渲染，后台线程跑）；俯视视角交给
 * {@link MapContours}。
 * <ul>
 *   <li>等距：画家算法按 U + V 由远及近逐列画，同一斜行互不重叠；每列画顶面菱形，再按 {@link MapColumns#depth} 只画朝左
 *       （+V）、朝右（+U）露出的那段崖面；像素归属与 {@link MapProjection#face} 完全一致，拾取据此反查。</li>
 *   <li>迷雾：没下发的区块画成迷雾平面（参照平面、按区块棋盘两色），已知列靠近迷雾 {@value MapGrid#FADE_BLOCKS} 格内用
 *       4×4 有序抖动按 {@link MapGrid#fade} 渐隐成迷雾色；抖动按全局像素取，平移不闪。</li>
 *   <li>虚空（已割弃、疆域外）透明不画，邻列朝它垂 {@value MapColumns#VOID_SKIRT} 格裙边；地图片缺失只会多出迷雾，
 *       不会抛异常。</li>
 *   <li>昼夜色调不烘进像素；输出原点与尺寸用全局地图像素。</li>
 * </ul>
 */

public final class MapRaster {

    private static final int[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };

    private MapRaster() {}

    public record Label(int x, int y, int level) {}

    public record Result(int originX, int originY, int width, int height, int[] pixels, List<Label> labels) {}

    public static Result draw(MapProjection projection, MapGrid grid, int fogPlane, int originX, int originY,
                              int width, int height) {
        Canvas canvas = new Canvas(originX, originY, Math.max(1, width), Math.max(1, height));
        if (projection.plan()) {
            List<Label> labels = MapContours.draw(canvas, projection, grid);
            return new Result(originX, originY, canvas.width, canvas.height, canvas.pixels, labels);
        }
        drawIso(canvas, projection, grid, fogPlane);
        return new Result(originX, originY, canvas.width, canvas.height, canvas.pixels, List.of());
    }

    //region Isometric [等距]
    private static void drawIso(Canvas canvas, MapProjection projection, MapGrid grid, int fogPlane) {
        int quarter = projection.quarter();
        int cell = projection.cell();
        int low = Math.min(grid.minPlane(), fogPlane) - MapColumns.VOID_SKIRT;
        int high = Math.max(grid.maxPlane(), fogPlane);
        int diagonalMin = Math.floorDiv(canvas.originX, cell) - 1;
        int diagonalMax = Math.floorDiv(canvas.originX + canvas.width, cell) + 1;
        int sumMin = Math.floorDiv(canvas.originY - cell + cell * low, quarter) - 1;
        int sumMax = Math.floorDiv(canvas.originY + canvas.height + cell * high, quarter) + 1;
        for (int sum = sumMin; sum <= sumMax; sum++) {
            int start = diagonalMin + Math.floorMod(sum - diagonalMin, 2);
            for (int diagonal = start; diagonal <= diagonalMax; diagonal += 2) {
                drawColumn(canvas, projection, grid, fogPlane, (sum + diagonal) >> 1, (sum - diagonal) >> 1);
            }
        }
    }

    private static void drawColumn(Canvas canvas, MapProjection projection, MapGrid grid, int fogPlane, int u, int v) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = grid.tile(x >> 4, z >> 4);
        int plane;
        int top;
        int left;
        int right;
        float fade;
        if (tile == null) {
            plane = fogPlane;
            top = ((x >> 4) + (z >> 4) & 1) == 0 ? MapPalette.FOG : MapPalette.FOG_ALT;
            left = MapPalette.FOG_SIDE;
            right = MapPalette.FOG_SIDE;
            fade = 1.0F;
        } else {
            int index = MapTile.index(x, z);
            if (MapColumns.isVoid(tile, index)) return;
            plane = MapColumns.top(tile, index) + 1;
            MapTerrain terrain = MapColumns.terrain(tile, index);
            top = MapPalette.top(terrain);
            left = MapPalette.left(terrain);
            right = MapPalette.right(terrain);
            fade = grid.fade(x >> 4, z >> 4, x & 15, z & 15);
        }
        int cell = projection.cell();
        int anchorX = projection.anchorX(u, v);
        int anchorY = projection.anchorY(u, v, plane);
        if (anchorY >= canvas.originY + canvas.height || anchorX + cell <= canvas.originX
                || anchorX - cell >= canvas.originX + canvas.width) {
            return;
        }
        int leftDepth = MapColumns.depth(plane, neighbour(projection, grid, fogPlane, u, v + 1));
        int rightDepth = MapColumns.depth(plane, neighbour(projection, grid, fogPlane, u + 1, v));
        if (anchorY + cell + cell * Math.max(leftDepth, rightDepth) <= canvas.originY) return;

        for (int row = 0; row < cell; row++) {
            int half = projection.topHalf(row);
            canvas.span(anchorX - half, anchorX + half, anchorY + row, top, MapPalette.FOG, fade);
        }
        for (int column = -cell; column < 0; column++) {
            int start = anchorY + projection.leftStart(column);
            canvas.column(anchorX + column, start, start + leftDepth * cell, left, MapPalette.FOG_SIDE, fade);
        }
        for (int column = 0; column < cell; column++) {
            int start = anchorY + projection.rightStart(column);
            canvas.column(anchorX + column, start, start + rightDepth * cell, right, MapPalette.FOG_SIDE, fade);
        }
    }

    private static int neighbour(MapProjection projection, MapGrid grid, int fogPlane, int u, int v) {
        return MapColumns.plane(grid, projection.columnX(u, v), projection.columnZ(u, v), fogPlane);
    }
    //endregion

    /** 输出画布：全局像素坐标写入，越界即丢；{@code fade < 1} 时按有序抖动改写成迷雾色。 */
    static final class Canvas {

        final int originX;
        final int originY;
        final int width;
        final int height;
        final int[] pixels;

        Canvas(int originX, int originY, int width, int height) {
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.pixels = new int[width * height];
        }

        void span(int fromX, int toX, int y, int colour, int fog, float fade) {
            int row = y - originY;
            if (row < 0 || row >= height) return;
            int start = Math.max(fromX, originX);
            int end = Math.min(toX, originX + width);
            for (int x = start; x < end; x++) pixels[row * width + x - originX] = dither(x, y, colour, fog, fade);
        }

        void column(int x, int fromY, int toY, int colour, int fog, float fade) {
            int offset = x - originX;
            if (offset < 0 || offset >= width) return;
            int start = Math.max(fromY, originY);
            int end = Math.min(toY, originY + height);
            for (int y = start; y < end; y++) pixels[(y - originY) * width + offset] = dither(x, y, colour, fog, fade);
        }

        void darken(int x, int y, float factor) {
            int offset = x - originX;
            int row = y - originY;
            if (offset < 0 || row < 0 || offset >= width || row >= height) return;
            pixels[row * width + offset] = MapPalette.darken(pixels[row * width + offset], factor);
        }

        boolean contains(int x, int y) {
            return x >= originX && y >= originY && x < originX + width && y < originY + height;
        }

        static float threshold(int x, int y) { return (BAYER[(y & 3) << 2 | x & 3] + 0.5F) / 16.0F; }

        private static int dither(int x, int y, int colour, int fog, float fade) {
            return fade < 1.0F && fade < threshold(x, y) ? fog : colour;
        }
    }
}
