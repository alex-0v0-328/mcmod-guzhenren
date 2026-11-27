package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import org.jetbrains.annotations.Nullable;

/**
 * 地图片来源：按区块坐标取 {@link MapTile}，{@code null} 即没下发过（迷雾）；拾取直接读 {@code LandMapClient}，光栅读
 * 渲染线程拍下的 {@link MapGrid} 快照。
 */

@FunctionalInterface
public interface TileSource {

    @Nullable MapTile tile(int chunkX, int chunkZ);
}
