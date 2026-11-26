package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import org.jetbrains.annotations.Nullable;

/**
 * 地图上的经营占地[Plot]：田、规划田各占 1 个区块，牧场占以西北角区块为起点的 2×2 区块；纯函数，供工具条、对象卡片与
 * 割弃确认框共用。
 * <ul>
 *   <li>{@link #at}：某区块上是什么（同一区块至多一样东西，契约 §8）。</li>
 *   <li>{@link #pastureCorner}：光标所在方块坐标 → 让 2×2 区块以光标为中心的西北角区块坐标。</li>
 *   <li>{@link #voided}：某侧宽 {@code width} 区块的割弃条带会作废几片田、几片规划、几个牧场（牧场任一区块落进条带
 *       即作废）。</li>
 * </ul>
 */

public final class MapPlots {

    private MapPlots() {}

    public enum Kind { FIELD, PLANNED, PASTURE }

    /** 一处占地：牧场的 {@code chunkX}/{@code chunkZ} 是西北角区块，{@code index} 是它在牧场列表里的下标。 */
    public record Plot(Kind kind, int chunkX, int chunkZ, int index, int mature, int growing, int penned) {

        public int size() { return kind == Kind.PASTURE ? 2 : 1; }

        public boolean covers(int chunkX, int chunkZ) {
            return chunkX >= this.chunkX && chunkX < this.chunkX + size() && chunkZ >= this.chunkZ
                    && chunkZ < this.chunkZ + size();
        }
    }

    /** 割弃将作废的数量。 */
    public record Voided(int fields, int planned, int pastures) {

        public boolean isEmpty() { return fields == 0 && planned == 0 && pastures == 0; }
    }

    public static @Nullable Plot at(List<Plot> plots, int chunkX, int chunkZ) {
        for (Plot plot : plots) {
            if (plot.covers(chunkX, chunkZ)) return plot;
        }
        return null;
    }

    public static int pastureCorner(double block) { return (int) Math.floor((block + 8.0) / 16.0) - 1; }

    public static boolean inStrip(Territory territory, Territory.Side side, int width, int chunkX, int chunkZ) {
        if (!territory.contains(chunkX, chunkZ)) return false;
        return switch (side) {
            case NORTH -> chunkZ < territory.minChunkZ() + width;
            case SOUTH -> chunkZ > territory.maxChunkZ() - width;
            case WEST -> chunkX < territory.minChunkX() + width;
            case EAST -> chunkX > territory.maxChunkX() - width;
        };
    }

    public static Voided voided(List<Plot> plots, Territory territory, Territory.Side side, int width) {
        int fields = 0;
        int planned = 0;
        int pastures = 0;
        for (Plot plot : plots) {
            if (!touches(plot, territory, side, width)) continue;
            switch (plot.kind()) {
                case FIELD -> fields++;
                case PLANNED -> planned++;
                case PASTURE -> pastures++;
            }
        }
        return new Voided(fields, planned, pastures);
    }

    private static boolean touches(Plot plot, Territory territory, Territory.Side side, int width) {
        for (int dx = 0; dx < plot.size(); dx++) {
            for (int dz = 0; dz < plot.size(); dz++) {
                if (inStrip(territory, side, width, plot.chunkX() + dx, plot.chunkZ() + dz)) return true;
            }
        }
        return false;
    }
}
