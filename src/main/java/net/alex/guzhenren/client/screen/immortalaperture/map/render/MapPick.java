package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.jetbrains.annotations.Nullable;

/**
 * 地图拾取[Map Pick]：全局地图像素 → 画在那里的那根柱子（含崖面），与 {@link MapRaster} 用同一套
 * {@link MapProjection} 与 {@link MapColumns}，所以点中的就是看到的。
 * <ul>
 *   <li>等距：像素横坐标只落在两条相邻斜行上，两者奇偶不同；从近（U + V 大）往远逐行找，第一个按
 *       {@link MapProjection#face} 盖住该像素的列就是答案（一列内先试树冠、再树干、再底座，树冠底下的空隙透到更远的列）；竖向搜索范围按世界高度 {@value #LOWEST_PLANE}–
 *       {@value #HIGHEST_PLANE} 平面定，每次约 800 列、各查表 1–3 次。</li>
 *   <li>俯视：直接按格取整。</li>
 *   <li>迷雾列也能点中（{@link Hit#known} 为假、地貌 {@link MapTerrain#UNKNOWN}）；虚空与空白处返回 {@code null}。</li>
 * </ul>
 */

public final class MapPick {

    private static final int LOWEST_PLANE = -64 - MapColumns.VOID_SKIRT;
    private static final int HIGHEST_PLANE = 321;

    private MapPick() {}

    /** 拾取结果：{@code ground} 为真实地面 y，{@code top} 为点中那一段的顶 y，{@code terrain} 为那一段的地貌。 */
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
        for (int sum = sumMax; sum >= sumMin; sum--) {
            int candidate = ((sum - diagonal) & 1) == 0 ? diagonal : diagonal + 1;
            int u = (sum + candidate) >> 1;
            int v = (sum - candidate) >> 1;
            Hit hit = column(projection, tiles, fogPlane, u, v, pixelX, pixelY);
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

    private static @Nullable Hit column(MapProjection projection, TileSource tiles, int fogPlane, int u, int v,
                                        int pixelX, int pixelY) {
        int x = projection.columnX(u, v);
        int z = projection.columnZ(u, v);
        int left = MapColumns.solidPlane(tiles, projection.columnX(u, v + 1), projection.columnZ(u, v + 1), fogPlane);
        int right = MapColumns.solidPlane(tiles, projection.columnX(u + 1, v), projection.columnZ(u + 1, v), fogPlane);
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        if (tile == null) {
            MapProjection.Face face = face(projection, u, v, fogPlane, MapRaster.NO_BOTTOM, left, right, pixelX, pixelY);
            return face == null ? null : new Hit(x, z, fogPlane - 1, fogPlane - 1, MapTerrain.UNKNOWN, face, false);
        }
        int index = MapTile.index(x, z);
        if (MapColumns.isVoid(tile, index)) return null;
        int ground = tile.ground()[index];
        if (!MapColumns.floats(tile, index)) {
            int top = MapColumns.top(tile, index);
            MapProjection.Face face = face(projection, u, v, top + 1, MapRaster.NO_BOTTOM, left, right, pixelX, pixelY);
            return face == null ? null : new Hit(x, z, ground, top, MapColumns.terrain(tile, index), face, true);
        }
        int low = tile.low()[index];
        int surface = tile.surface()[index];
        MapProjection.Face face = face(projection, u, v, surface + 1, low, left, right, pixelX, pixelY);
        if (face != null) return new Hit(x, z, ground, surface, MapColumns.terrain(tile, index), face, true);
        if (MapColumns.trunk(tile, index)) {
            face = face(projection, u, v, low, ground + 1, left, right, pixelX, pixelY);
            if (face != null) return new Hit(x, z, ground, low - 1, MapTerrain.WOOD, face, true);
        }
        MapTerrain base = MapColumns.base(tile, index);
        if (base == MapTerrain.VOID) return null;
        face = face(projection, u, v, ground + 1, MapRaster.NO_BOTTOM, left, right, pixelX, pixelY);
        return face == null ? null : new Hit(x, z, ground, ground, base, face, true);
    }

    private static MapProjection.@Nullable Face face(MapProjection projection, int u, int v, int plane, int bottom,
                                                    int leftNeighbour, int rightNeighbour, int pixelX, int pixelY) {
        int column = pixelX - projection.anchorX(u, v);
        int row = pixelY - projection.anchorY(u, v, plane);
        if (row < 0) return null;
        if (row < projection.quarter()) return projection.face(column, row, 0, 0);

        return projection.face(column, row, MapRaster.exposed(plane, bottom, leftNeighbour),
                MapRaster.exposed(plane, bottom, rightNeighbour));
    }
}
