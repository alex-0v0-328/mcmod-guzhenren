package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 地图「选择」模式的选区：若干块田与规划田（按区块记），或单独一个牧场（按西北角区块记）；两者互斥，点牧场即换成只选它。
 * <ul>
 *   <li>按区块记而不按快照下标记：快照每秒整份替换，下标会因拆田、开垦而移位；规划田开垦成田后区块不变，仍在选区里。</li>
 *   <li>{@link #refresh} 每帧丢掉快照里已没有的区块；{@link #inBox} 是框选矩形内的田与规划田。</li>
 * </ul>
 */

final class MapSelection {

    private final Set<Long> chunks = new LinkedHashSet<>();
    private @Nullable Long pasture;
    private int revision;

    boolean isEmpty() { return chunks.isEmpty() && pasture == null; }

    boolean hasFields() { return !chunks.isEmpty(); }

    /** 选区每变一次加一，操作栏据此撤销「点两次确认」。 */
    int revision() { return revision; }

    void clear() {
        if (isEmpty()) return;
        chunks.clear();
        pasture = null;
        revision++;
    }

    boolean has(MapPlots.Plot plot) {
        long key = ChunkPos.asLong(plot.chunkX(), plot.chunkZ());
        return plot.kind() == MapPlots.Kind.PASTURE ? pasture != null && pasture == key : chunks.contains(key);
    }

    void only(MapPlots.Plot plot) {
        chunks.clear();
        pasture = null;
        add(plot);
        revision++;
    }

    /** Shift 点：田与规划田加入或移出；牧场不与田同选，改为只选它。 */
    void toggle(MapPlots.Plot plot) {
        if (plot.kind() == MapPlots.Kind.PASTURE) {
            only(plot);
            return;
        }
        pasture = null;
        long key = ChunkPos.asLong(plot.chunkX(), plot.chunkZ());
        if (!chunks.remove(key)) chunks.add(key);
        revision++;
    }

    void addAll(List<MapPlots.Plot> plots) {
        pasture = null;
        for (MapPlots.Plot plot : plots) add(plot);
        revision++;
    }

    private void add(MapPlots.Plot plot) {
        long key = ChunkPos.asLong(plot.chunkX(), plot.chunkZ());
        if (plot.kind() == MapPlots.Kind.PASTURE) pasture = key;
        else chunks.add(key);
    }

    void refresh(List<MapPlots.Plot> plots) {
        boolean changed = chunks.removeIf(key -> {
            MapPlots.Plot plot = MapPlots.at(plots, ChunkPos.getX(key), ChunkPos.getZ(key));
            return plot == null || plot.kind() == MapPlots.Kind.PASTURE;
        });
        if (pasture != null) {
            MapPlots.Plot plot = MapPlots.at(plots, ChunkPos.getX(pasture), ChunkPos.getZ(pasture));
            if (plot == null || plot.kind() != MapPlots.Kind.PASTURE) {
                pasture = null;
                changed = true;
            }
        }
        if (changed) revision++;
    }

    List<Long> chunks() { return List.copyOf(chunks); }

    /** 选中的田与规划田，按占地表顺序。 */
    List<MapPlots.Plot> plots(List<MapPlots.Plot> plots) {
        List<MapPlots.Plot> chosen = new ArrayList<>();
        for (MapPlots.Plot plot : plots) {
            if (plot.kind() != MapPlots.Kind.PASTURE && has(plot)) chosen.add(plot);
        }
        return chosen;
    }

    MapPlots.@Nullable Plot pasture(List<MapPlots.Plot> plots) {
        if (pasture == null) return null;
        MapPlots.Plot plot = MapPlots.at(plots, ChunkPos.getX(pasture), ChunkPos.getZ(pasture));
        return plot != null && plot.kind() == MapPlots.Kind.PASTURE ? plot : null;
    }

    static List<MapPlots.Plot> inBox(List<MapPlots.Plot> plots, int fromX, int fromZ, int toX, int toZ) {
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        int minZ = Math.min(fromZ, toZ);
        int maxZ = Math.max(fromZ, toZ);
        List<MapPlots.Plot> inside = new ArrayList<>();
        for (MapPlots.Plot plot : plots) {
            if (plot.kind() == MapPlots.Kind.PASTURE) continue;
            if (plot.chunkX() >= minX && plot.chunkX() <= maxX && plot.chunkZ() >= minZ && plot.chunkZ() <= maxZ) {
                inside.add(plot);
            }
        }
        return inside;
    }
}
