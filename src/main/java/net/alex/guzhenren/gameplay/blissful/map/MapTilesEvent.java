package net.alex.guzhenren.gameplay.blissful.map;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 地图片下发事件：{@link LandMaps} 给订阅者凑好一包地图片（至多 {@value LandMaps#TILES_PER_PACKET} 片）时同步发布，
 * 由网络层转成下发通知。
 */

public class MapTilesEvent extends Event {

    private final ServerPlayer player;
    private final List<MapTile> tiles;

    public MapTilesEvent(@NotNull ServerPlayer player, @NotNull List<MapTile> tiles) {
        this.player = player;
        this.tiles = List.copyOf(tiles);
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull List<MapTile> getTiles() { return tiles; }
}
