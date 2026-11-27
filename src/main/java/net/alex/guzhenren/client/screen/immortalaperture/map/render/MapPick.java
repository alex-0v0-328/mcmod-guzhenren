package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.jetbrains.annotations.Nullable;

/**
 * 地图拾取[Map Pick]：全局地图像素 → 画在那里的那根柱子（含崖面），与 {@link MapRaster} 用同一套
 * {@link MapProjection} 与 {@link MapColumns}，所以点中的就是看到的。
 * <ul>
 *   <li>等距：像素横坐标只落在两条相邻斜行上，两者奇偶不同；从近（U + V 大）往远逐行找，第一个按
 *       {@link MapProjection#face} 盖住该像素的列就是答案；竖向搜索范围按世界高度 {@value #LOWEST_PLANE}–
 *       {@value #HIGHEST_PLANE} 平面定，每次约 800 列、各查表 1–3 次。</li>
 *   <li>俯视：直接按格取整。</li>
 *   <li>迷雾列也能点中（{@link Hit#known} 为假、地貌 {@link MapTerrain#UNKNOWN}）；虚空与空白处返回 {@code null}。</li>
 * </ul>
 */

public final class MapPick {

    private static final int LOWEST_PLANE = -64 - MapColumns.VOID_SKIRT;
    private static final int HIGHEST_PLANE = 321;

    private MapPick() {}

    /** 拾取结果：{@code ground} 为真实地面 y，{@code top} 为柱顶 y。 */
    public record Hit(int x, int z, int ground, int top, MapTerrain terrain, MapProjection.Face face, boolean known) {}

    public static @Nullable Hit pick(MapProjection projection, TileSource tiles, int fogPlane, int pixelX, int pixelY) {
        if (projection.plan()) {
            int cell = projection.cell();
            return hit(projection, tiles, fogPlane, Math.floorDiv(pixelX, cell), Math.floorDiv(pixelY, cell),
                    MapProjection.Face.TOP);
        }
        int quarter = projection.quarter();
        int cell = projection.cell();
        int low = Math.min(LOWEST_PLANE, fogPlane - MapColumns.VOID_SKIRT);
        int high = Math.max(HIGHEST_PLANE, fogPlane);
        int sumMin = Math.floorDiv(pixelY - cell + cell * low, quarter) - 1;
        int sumMax = Math.floorDiv(pixelY + cell * high, quarter) + 1;
        int diagonal = Math.floorDiv(pixelX, cell);
        for (int sum = sumMax; sum >= sumMin; sum--) {
            int candidate = ((sum - diagonal) & 1) == 0 ? diagonal : diagonal + 1;
            int u = (sum + candidate) >> 1;
            int v = (sum - candidate) >> 1;
            MapProjection.Face face = face(projection, tiles, fogPlane, u, v, pixelX, pixelY);
            if (face != null) return hit(projection, tiles, fogPlane, u, v, face);
        }
        return null;
    }

    private static MapProjection.@Nullable Face face(MapProjection projection, TileSource tiles, int fogPlane, int u,
                                                    int v, int pixelX, int pixelY) {
        int plane = MapColumns.plane(tiles, projection.columnX(u, v), projection.columnZ(u, v), fogPlane);
        if (plane == MapColumns.VOID) return null;
        int column = pixelX - projection.anchorX(u, v);
        int row = pixelY - projection.anchorY(u, v, plane);
        if (row < 0) return null;
        if (row < projection.quarter()) return projection.face(column, row, 0, 0);

        int left = MapColumns.depth(plane,
                MapColumns.plane(tiles, projection.columnX(u, v + 1), projection.columnZ(u, v + 1), fogPlane));
        int right = MapColumns.depth(plane,
                MapColumns.plane(tiles, projection.columnX(u + 1, v), projection.columnZ(u + 1, v), fogPlane));
        return projection.face(column, row, left, right);
    }

    private static @Nullable Hit hit(MapProjection projection, TileSource tiles, int fogPlane, int u, int v,
                                     MapProjection.Face face) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        if (tile == null) return new Hit(x, z, fogPlane - 1, fogPlane - 1, MapTerrain.UNKNOWN, face, false);
        int index = MapTile.index(x, z);
        if (MapColumns.isVoid(tile, index)) return null;
        return new Hit(x, z, tile.ground()[index], MapColumns.top(tile, index), MapColumns.terrain(tile, index), face,
                true);
    }
}
