package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;

/**
 * 地图列的读法：光栅与拾取共用，保证画出来的和点中的是同一根柱子。
 * <ul>
 *   <li>{@link #top}：树叶、木头、栅栏这类高出地面的（{@link #raised}）按 {@code surface} 立柱，其余按真实地面
 *       {@code ground}（作物、花压平在地面上）；{@link #terrain}：流体列除岩浆与立柱类外当水，⚠ 悬在水面上的树冠扫描时
 *       先碰到水也带流体标志，不排除立柱类会按树冠高度画成蓝柱。</li>
 *   <li>{@link #plane}：顶面平面 = 顶层 y + 1；没下发的区块是迷雾，平面取参照的迷雾平面；已割弃或疆域外的列返回
 *       {@link #VOID}（不画、拾不中）。</li>
 *   <li>{@link #depth}：本列朝某邻列露出的崖面格数，邻列是虚空时固定垂 {@value #VOID_SKIRT} 格裙边。</li>
 * </ul>
 */

public final class MapColumns {

    public static final int VOID = Integer.MIN_VALUE;
    public static final int VOID_SKIRT = 8;

    private MapColumns() {}

    public static MapTerrain terrain(MapTile tile, int index) {
        MapTerrain terrain = MapTerrain.byId(tile.terrain()[index]);
        if ((tile.flags()[index] & MapTile.FLAG_FLUID) == 0 || terrain == MapTerrain.LAVA || raised(terrain)) {
            return terrain;
        }
        return MapTerrain.WATER;
    }

    public static boolean raised(MapTerrain terrain) {
        return terrain == MapTerrain.FOLIAGE || terrain == MapTerrain.WOOD || terrain == MapTerrain.FENCE;
    }

    public static boolean isVoid(MapTile tile, int index) { return tile.terrain()[index] == MapTerrain.VOID.id(); }

    public static int top(MapTile tile, int index) {
        int ground = tile.ground()[index];
        return raised(MapTerrain.byId(tile.terrain()[index])) ? Math.max(ground, tile.surface()[index]) : ground;
    }

    public static int plane(TileSource tiles, int x, int z, int fogPlane) {
        MapTile tile = tiles.tile(x >> 4, z >> 4);
        if (tile == null) return fogPlane;
        int index = MapTile.index(x, z);
        return isVoid(tile, index) ? VOID : top(tile, index) + 1;
    }

    public static int depth(int plane, int neighbourPlane) {
        return neighbourPlane == VOID ? VOID_SKIRT : Math.max(0, plane - neighbourPlane);
    }
}
