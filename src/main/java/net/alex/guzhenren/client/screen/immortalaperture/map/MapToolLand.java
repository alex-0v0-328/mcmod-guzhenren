package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.FarmPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.HerdPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页的经营数据来源：把 {@code MapPart.Section} 的田、规划田、牧场换成 {@link MapPlots.Plot}，预判走
 * {@link Siting}（地形读 {@link LandMapClient#tile}，没下发的区块即迷雾、不许选址，左键改为探明），操作发 {@code FarmPart} 与
 * {@code HerdPart} 的意图，服务端全部复判；对象栏读的田、牧场行按下标取快照段里的原记录，选中田的操作带区块列表（至多
 * {@code FarmPart.LIMIT} 个）。
 * <ul>
 *   <li>快照段每秒或每次操作后整份替换；按引用比对，换了才重建占地表与 {@link Siting.Plot}。</li>
 *   <li>移动预判不算原占地：牧场用 {@link Siting.Plot#without} 排除原 4 个区块，田用 {@link Siting.Plot#withoutField}。</li>
 * </ul>
 */

final class MapToolLand implements MapTools.Land {

    private final MapPage page;
    private MapPart.Section seen = MapPart.Section.EMPTY;
    private List<MapPlots.Plot> plots = List.of();
    private Siting.Plot siting = MapPart.Section.EMPTY.siting();

    MapToolLand(MapPage page) { this.page = page; }

    private MapPart.Section section() {
        MapPart.Section section = page.data();
        if (section != seen) {
            seen = section;
            plots = plots(section);
            siting = section.siting();
        }
        return section;
    }

    private static List<MapPlots.Plot> plots(MapPart.Section section) {
        List<MapPlots.Plot> result = new ArrayList<>();
        for (int i = 0; i < section.fields().size(); i++) {
            MapPart.Section.Field field = section.fields().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.FIELD, field.chunkX(), field.chunkZ(), i, field.mature(),
                    field.growing(), 0));
        }
        for (int i = 0; i < section.planned().size(); i++) {
            ChunkPos planned = section.planned().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.PLANNED, planned.x, planned.z, i, 0, 0, 0));
        }
        for (int i = 0; i < section.pastures().size(); i++) {
            HerdPart.Row pen = section.pastures().get(i);
            result.add(new MapPlots.Plot(MapPlots.Kind.PASTURE, pen.chunkX(), pen.chunkZ(), i, 0, 0, pen.penned()));
        }
        return List.copyOf(result);
    }

    private static MapTools.Verdict verdict(Siting.Reason reason) {
        return reason == Siting.Reason.OK ? MapTools.Verdict.FIT
                : new MapTools.Verdict(false, Component.translatable(reason.key()), reason == Siting.Reason.FOG);
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
    public MapPart.Section.@Nullable Field field(int index) {
        List<MapPart.Section.Field> fields = section().fields();
        return index >= 0 && index < fields.size() ? fields.get(index) : null;
    }

    @Override
    public HerdPart.@Nullable Row pen(int index) {
        List<HerdPart.Row> pens = section().pastures();
        return index >= 0 && index < pens.size() ? pens.get(index) : null;
    }

    @Override
    public long mud() { return section().mud(); }

    @Override
    public long boneMeal() { return section().boneMeal(); }

    @Override
    public long seeds(int crop) { return section().seedsOf(crop); }

    @Override
    public List<HerdPart.Kind> animals() { return section().animals(); }

    @Override
    public MapTools.Verdict farm(int chunkX, int chunkZ) {
        section();
        return verdict(Siting.field(siting, LandMapClient::tile, chunkX, chunkZ));
    }

    @Override
    public MapTools.Verdict pasture(int chunkX, int chunkZ, int ignoredIndex) {
        MapPart.Section section = section();
        Siting.Plot plot = siting;
        if (ignoredIndex >= 0 && ignoredIndex < section.pastures().size()) {
            HerdPart.Row pen = section.pastures().get(ignoredIndex);
            plot = siting.without(pen.chunkX(), pen.chunkZ());
        }
        return verdict(Siting.pasture(plot, LandMapClient::tile, chunkX, chunkZ));
    }

    @Override
    public MapTools.Verdict moveField(int fromX, int fromZ, int chunkX, int chunkZ) {
        section();
        return verdict(Siting.field(siting.withoutField(fromX, fromZ), LandMapClient::tile, chunkX, chunkZ));
    }

    @Override
    public void plan(int chunkX, int chunkZ) { page.sendAction(new FarmPart.Plan(chunkX, chunkZ)); }

    @Override
    public void planMany(List<Long> chunks) {
        if (!chunks.isEmpty()) page.sendAction(new FarmPart.PlanMany(List.copyOf(chunks)));
    }

    @Override
    public void sow(List<Long> chunks, int crop) { page.sendAction(new FarmPart.Sow(limit(chunks), crop)); }

    @Override
    public void retype(List<Long> chunks, int kind) { page.sendAction(new FarmPart.Retype(limit(chunks), kind)); }

    @Override
    public void ripen(List<Long> chunks) { page.sendAction(new FarmPart.Ripen(limit(chunks))); }

    @Override
    public void harvest(List<Long> chunks) { page.sendAction(new FarmPart.HarvestFields(limit(chunks))); }

    @Override
    public void uproot(List<Long> chunks) { page.sendAction(new FarmPart.Uproot(limit(chunks))); }

    @Override
    public void clear(List<Long> chunks) { page.sendAction(new FarmPart.Clear(limit(chunks))); }

    private static List<Long> limit(List<Long> chunks) {
        return chunks.size() <= FarmPart.LIMIT ? List.copyOf(chunks) : List.copyOf(chunks.subList(0, FarmPart.LIMIT));
    }

    @Override
    public void placePasture(int chunkX, int chunkZ) { page.sendAction(new HerdPart.PlacePasture(chunkX, chunkZ)); }

    @Override
    public void movePasture(int index, int chunkX, int chunkZ) {
        page.sendAction(new HerdPart.MovePasture(index, chunkX, chunkZ));
    }

    @Override
    public void removePasture(int index) { page.sendAction(new HerdPart.RemovePasture(index)); }

    @Override
    public void reveal(int chunkX, int chunkZ, int side) {
        page.sendAction(new MapPart.RevealChunk(chunkX, chunkZ, side));
    }

    @Override
    public void teleport(int x, int z) { page.sendAction(new MapPart.Teleport(x, z)); }


    @Override
    public void gather(int index) { page.sendAction(new HerdPart.Gather(index)); }

    @Override
    public void sendMoveField(int fromX, int fromZ, int chunkX, int chunkZ) {
        page.sendAction(new FarmPart.MoveField(fromX, fromZ, chunkX, chunkZ));
    }

    @Override
    public void setAnimal(int index, Optional<ResourceLocation> animal) {
        page.sendAction(new HerdPart.SetAnimal(index, animal));
    }

    @Override
    public void setFloor(int index, int floor) { page.sendAction(new HerdPart.SetFloor(index, floor)); }

    @Override
    public void slaughter(int index) { page.sendAction(new HerdPart.SlaughterPen(index)); }
}
