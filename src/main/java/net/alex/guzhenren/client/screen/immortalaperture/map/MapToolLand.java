package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmModule;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdModule;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapModule;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

/**
 * 地图页的经营数据来源：把 {@code MapModule.Section} 的田、规划田、牧场换成 {@link MapPlots.Plot}，预判走
 * {@link Siting}（地形读 {@link LandMapClient#tile}，没下发的区块即迷雾、不许选址），操作发 {@code FarmModule} 与
 * {@code HerdModule} 的意图，服务端全部复判。
 * <ul>
 *   <li>快照段每秒或每次操作后整份替换；按引用比对，换了才重建占地表与 {@link Siting.Plot}。</li>
 *   <li>迁移预判用 {@link Siting.Plot#without} 排除原牧场的 4 个区块。</li>
 * </ul>
 */

final class MapToolLand implements MapTools.Land {

    private final MapPage page;
    private MapModule.Section seen = MapModule.Section.EMPTY;
    private List<MapPlots.Plot> plots = List.of();
    private Siting.Plot siting = MapModule.Section.EMPTY.siting();

    MapToolLand(MapPage page) { this.page = page; }

    private MapModule.Section section() {
        MapModule.Section section = page.data();
        if (section != seen) {
            seen = section;
            plots = plots(section);
            siting = section.siting();
        }
        return section;
    }

    private static List<MapPlots.Plot> plots(MapModule.Section section) {
        List<MapPlots.Plot> result = new ArrayList<>();
        for (int i = 0; i < section.fields().size(); i++) {
            MapModule.Section.Field field = section.fields().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.FIELD, field.chunkX(), field.chunkZ(), i, field.mature(),
                    field.growing(), 0));
        }
        for (int i = 0; i < section.planned().size(); i++) {
            ChunkPos planned = section.planned().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.PLANNED, planned.x, planned.z, i, 0, 0, 0));
        }
        for (int i = 0; i < section.pastures().size(); i++) {
            MapModule.Section.Pen pen = section.pastures().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.PASTURE, pen.chunkX(), pen.chunkZ(), i, 0, 0, pen.penned()));
        }
        return List.copyOf(result);
    }

    private static MapTools.Verdict verdict(Siting.Reason reason) {
        return reason == Siting.Reason.OK ? MapTools.Verdict.FIT
                : new MapTools.Verdict(false, Component.translatable(reason.key()));
    }

    @Override
    public List<MapPlots.Plot> plots() {
        section();
        return plots;
    }

    @Override
    public int fieldCap() { return section().fieldCap(); }

    @Override
    public int pastureCap() { return section().pastureCap(); }

    @Override
    public int gatherSeconds() { return section().gatherSeconds(); }

    @Override
    public MapTools.Verdict farm(int chunkX, int chunkZ) {
        section();
        return verdict(Siting.field(siting, LandMapClient::tile, chunkX, chunkZ));
    }

    @Override
    public MapTools.Verdict pasture(int chunkX, int chunkZ, int ignoredIndex) {
        MapModule.Section section = section();
        Siting.Plot plot = siting;
        if (ignoredIndex >= 0 && ignoredIndex < section.pastures().size()) {
            MapModule.Section.Pen pen = section.pastures().get(ignoredIndex);
            plot = siting.without(pen.chunkX(), pen.chunkZ());
        }
        return verdict(Siting.pasture(plot, LandMapClient::tile, chunkX, chunkZ));
    }

    @Override
    public void plan(int chunkX, int chunkZ) { page.sendAction(new FarmModule.Plan(chunkX, chunkZ)); }

    @Override
    public void unplan(int chunkX, int chunkZ) { page.sendAction(new FarmModule.Unplan(chunkX, chunkZ)); }

    @Override
    public void remove(int chunkX, int chunkZ) { page.sendAction(new FarmModule.Remove(chunkX, chunkZ)); }

    @Override
    public void placePasture(int chunkX, int chunkZ) { page.sendAction(new HerdModule.PlacePasture(chunkX, chunkZ)); }

    @Override
    public void movePasture(int index, int chunkX, int chunkZ) {
        page.sendAction(new HerdModule.MovePasture(index, chunkX, chunkZ));
    }

    @Override
    public void removePasture(int index) { page.sendAction(new HerdModule.RemovePasture(index)); }

    @Override
    public void gather(int index) { page.sendAction(new HerdModule.Gather(index)); }
}
