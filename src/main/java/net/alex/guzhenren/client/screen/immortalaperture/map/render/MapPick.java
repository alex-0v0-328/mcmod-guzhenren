package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.jetbrains.annotations.Nullable;

/**
 * 地图拾取[Map Pick]：全局地图像素 → 画在那里的那一段方块（含崖面），与 {@link MapRaster} 用同一套
 * {@link MapProjection} 与 {@link MapColumns}，所以点中的就是看到的。
 * <ul>
 *   <li>等距：像素横坐标只落在两条相邻斜行上，两者奇偶不同；从近（U + V 大）往远逐行找，第一个盖住该像素的段就是答案
 *       （一列内从上往下试各段，段间空气透到更远的列，崖面只认 {@link MapColumns#exposed} 露出的那几截）；竖向搜索范围按
 *       世界高度 {@value #LOWEST_PLANE}–{@value #HIGHEST_PLANE} 平面定，每次约 800 列、每段量一次两侧崖面。</li>
 *   <li>俯视：直接按格取整。</li>
 *   <li>迷雾列也能点中（{@link Hit#known} 为假、地貌 {@link MapTerrain#UNKNOWN}）；虚空与空白处返回 {@code null}。</li>
 * </ul>
 */

public final class MapPick {

    private static final int LOWEST_PLANE = -64 - MapColumns.VOID_SKIRT;
    private static final int HIGHEST_PLANE = 321;

    private MapPick() {}

    /** 拾取结果：{@code ground} 为该列真实地面 y，{@code top} 为点中那一段的顶 y，{@code terrain} 为那一段的地貌。 */
    public record Hit(int x, int z, int ground, int top, MapTerrain terrain, MapProjection.Face face, boolean known) {}

    public static @Nullable Hit pick(MapProjection projection, TileSource tiles, int fogPlane, int pixelX, int pixelY) {
        if (projection.plan()) {
            int cell = projection.cell();
            return plan(projection, tiles, fogPlane, Math.floorDiv(pixelX, cell), Math.floorDiv(pixelY, cell));
        }
        int quarter = projection.quarter();
        int cell = projection.cell();
        int low = Math.min(LOWEST_PLANE, fogPlane - MapColumns.VOID_SKIRT);
        int high = Math.max(HIGHEST_PLANE, fogPlane);
        int sumMin = Math.floorDiv(pixelY - cell + cell * low, quarter) - 1;
        int sumMax = Math.floorDiv(pixelY + cell * high, quarter) + 1;
        int diagonal = Math.floorDiv(pixelX, cell);
        MapRaster.Faces faces = new MapRaster.Faces();
        for (int sum = sumMax; sum >= sumMin; sum--) {
            int candidate = ((sum - diagonal) & 1) == 0 ? diagonal : diagonal + 1;
            int u = (sum + candidate) >> 1;
            int v = (sum - candidate) >> 1;
            Hit hit = column(projection, tiles, fogPlane, faces, u, v, pixelX, pixelY);
            if (hit != null) return hit;
        }
        return null;
    }

    private static @Nullable Hit plan(MapProjection projection, TileSource tiles, int fogPlane, int u, int v) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        MapProjection.Face top = MapProjection.Face.TOP;
        if (tile == null) return new Hit(x, z, fogPlane - 1, fogPlane - 1, MapTerrain.UNKNOWN, top, false);
        int index = MapTile.index(x, z);
        if (MapColumns.isVoid(tile, index)) return null;
        return new Hit(x, z, tile.ground()[index], MapColumns.top(tile, index), MapColumns.terrain(tile, index), top,
                true);
    }

    private static @Nullable Hit column(MapProjection projection, TileSource tiles, int fogPlane,
                                        MapRaster.Faces faces, int u, int v, int pixelX, int pixelY) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        if (tile == null) {
            faces.measure(projection, tiles, fogPlane, u, v, fogPlane - 1, MapTile.OPEN);
            MapProjection.Face face = face(projection, faces, u, v, fogPlane, pixelX, pixelY);
            return face == null ? null : new Hit(x, z, fogPlane - 1, fogPlane - 1, MapTerrain.UNKNOWN, face, false);
        }
        int index = MapTile.index(x, z);
        for (int k = 0; k < tile.runs(index); k++) {
            int run = tile.run(index, k);
            int top = tile.runTop(run);
            if (pixelY < projection.anchorY(u, v, top + 1)) continue;
            faces.measure(projection, tiles, fogPlane, u, v, top, tile.runBottom(run));
            MapProjection.Face face = face(projection, faces, u, v, top + 1, pixelX, pixelY);
            if (face != null) return new Hit(x, z, tile.ground()[index], top, tile.runTerrain(run), face, true);
        }
        return null;
    }

    private static MapProjection.@Nullable Face face(MapProjection projection, MapRaster.Faces faces, int u, int v,
                                                    int plane, int pixelX, int pixelY) {
        int column = pixelX - projection.anchorX(u, v);
        int row = pixelY - projection.anchorY(u, v, plane);
        if (row < 0) return null;
        if (projection.face(column, row, 0, 0) == MapProjection.Face.TOP) return MapProjection.Face.TOP;

        int cell = projection.cell();
        if (column >= -cell && column < 0) {
            int start = projection.leftStart(column);
            return row >= start && MapRaster.Faces.covers(faces.left, faces.leftCount, (row - start) / cell)
                    ? MapProjection.Face.LEFT : null;
        }
        if (column >= 0 && column < cell) {
            int start = projection.rightStart(column);
            return row >= start && MapRaster.Faces.covers(faces.right, faces.rightCount, (row - start) / cell)
                    ? MapProjection.Face.RIGHT : null;
        }
        return null;
    }
}
