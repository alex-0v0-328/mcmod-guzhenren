package net.alex.guzhenren.gameplay.blissful.map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import org.jetbrains.annotations.NotNull;

/**
 * 一个订阅者的地图状态（只在内存）：看的哪块福地、视口、按距离排好的区块序、发过的地图片哈希、待发队列。
 * <ul>
 *   <li>{@link #setView}：换视口时按 {@link MapView#keeps} 忘掉远处「已发过」的记录（客户端同规则丢片），待发队列只留
 *       新视口内的；返回新视口里还没发过的区块（近到远），交给 {@link LandMap} 排扫描。</li>
 *   <li>{@link #offer}：地图片哈希与已发的不同才进待发队列（同区块后来的覆盖先来的）；{@link #drain} 每次至多取
 *       {@code limit} 片并记为已发。</li>
 *   <li>{@link #exploreCursor} 是探索在 {@link #order} 上走到的位置，换视口与每轮重扫时归零。</li>
 * </ul>
 */

final class MapViewer {

    final UUID player;
    final BlissfulLand land;
    MapView view = MapView.NONE;
    long[] order = new long[0];
    int exploreCursor;
    long nextRescan;
    long nextMarks;
    private final Long2IntOpenHashMap sent = new Long2IntOpenHashMap();
    private final Map<Long, MapTile> outbox = new LinkedHashMap<>();

    MapViewer(@NotNull UUID player, @NotNull BlissfulLand land) {
        this.player = player;
        this.land = land;
    }

    @NotNull List<Long> setView(@NotNull MapView view) {
        this.view = view;
        this.order = view.byDistance();
        this.exploreCursor = 0;
        this.sent.keySet().removeIf((long chunk) -> !view.keeps(chunk));
        this.outbox.keySet().removeIf(chunk -> !view.contains(chunk));
        List<Long> unsent = new ArrayList<>();
        for (long chunk : this.order) {
            if (!this.sent.containsKey(chunk)) unsent.add(chunk);
        }
        return unsent;
    }

    boolean wants(long chunk) { return this.view.contains(chunk); }

    void offer(@NotNull MapTile tile) {
        long chunk = tile.chunk();
        if (!wants(chunk)) return;
        if (this.sent.containsKey(chunk) && this.sent.get(chunk) == tile.hash()) {
            this.outbox.remove(chunk);
            return;
        }
        this.outbox.put(chunk, tile);
    }

    boolean hasOutbox() { return !this.outbox.isEmpty(); }

    @NotNull List<MapTile> drain(int limit) {
        List<MapTile> tiles = new ArrayList<>(Math.min(limit, this.outbox.size()));
        Iterator<MapTile> iterator = this.outbox.values().iterator();
        while (iterator.hasNext() && tiles.size() < limit) {
            MapTile tile = iterator.next();
            iterator.remove();
            this.sent.put(tile.chunk(), tile.hash());
            tiles.add(tile);
        }
        return tiles;
    }
}
