package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureSettings;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.LandMaps;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSite;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 「地图」页面板段：下发地图页叠加层与卡片需要的经营数据（疆域、山心与秘境半径、行宫、入口、田与其作物田型、规划、
 * 牧场与其设置、选址上限、库藏腐泥冻土数、可勾选的收拢种类）；
 * 地形片与活物标记另走地图载荷，不在这一段。选址操作在 {@code FarmPart}、{@code HerdPart}；本段只有「探明」
 * （{@link RevealChunk}，拿着选址工具点迷雾，交 {@link LandMaps#reveal} 限流后生成并加载）。
 * <ul>
 *   <li>秘境未解出时山心、行宫、入口都是 {@link BlockPos#ZERO}、秘境半径 0（{@link Siting} 视作不查秘境）。</li>
 *   <li>{@link #plot}（服务端复判）与 {@link Section#siting}（客户端预判）取同一组占地：田、规划、牧场 4 区块；服务端
 *       地形读 {@link Siting#loadedTiles}。</li>
 *   <li>{@link Section#animals()} = 全福地已加载可支使动物的种类（捕食者除外，收拢本就不带），加上任一牧场设成不收的
 *       种类，按 ID 排序；牧场卡片的收拢种类列表就列这些。</li>
 * </ul>
 */

public final class MapPart implements PanelSectionSource<MapPart.Section> {

    public static final MapPart INSTANCE = new MapPart();

    private MapPart() {}

    public record RevealChunk(int chunkX, int chunkZ, int side) implements PanelAction {

        public static final PanelAction.Type<RevealChunk> TYPE = new PanelAction.Type<>(RevealChunk.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, RevealChunk::chunkX, ByteBufCodecs.VAR_INT,
                        RevealChunk::chunkZ, ByteBufCodecs.VAR_INT, RevealChunk::side, RevealChunk::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return LandMaps.reveal(context.player(), chunkX, chunkZ, side, EarthSpiritPanel::mapViewerLand)
                    ? Component.empty() : LandNoticeEvent.text("reveal_busy");
        }
    }

    public record Section(@NotNull Territory territory, @NotNull BlockPos mountain, int realmRadius,
                          @NotNull BlockPos palace, @NotNull BlockPos entry, @NotNull List<Field> fields,
                          @NotNull List<ChunkPos> planned, @NotNull List<Pen> pastures, int fieldCap, int pastureCap,
                          long mud, @NotNull List<ResourceLocation> animals) implements PanelSection {

        public record Field(int chunkX, int chunkZ, int mature, int growing, int crop, int kind) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Field> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Field::chunkX,
                    ByteBufCodecs.VAR_INT, Field::chunkZ,
                    ByteBufCodecs.VAR_INT, Field::mature,
                    ByteBufCodecs.VAR_INT, Field::growing,
                    ByteBufCodecs.VAR_INT, Field::crop,
                    ByteBufCodecs.VAR_INT, Field::kind,
                    Field::new);
        }

        public record Pen(int chunkX, int chunkZ, int penned, boolean autoGather,
                          @NotNull List<ResourceLocation> excluded) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Pen> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Pen::chunkX,
                    ByteBufCodecs.VAR_INT, Pen::chunkZ,
                    ByteBufCodecs.VAR_INT, Pen::penned,
                    ByteBufCodecs.BOOL, Pen::autoGather,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Pen::excluded,
                    Pen::new);

            public boolean gathers(@NotNull ResourceLocation type) { return !excluded.contains(type); }
        }

        public static final Section EMPTY = new Section(TerritoryPart.Section.EMPTY.territory(), BlockPos.ZERO, 0,
                BlockPos.ZERO, BlockPos.ZERO, List.of(), List.of(), List.of(), Siting.FIELD_CAP, Siting.PASTURE_CAP,
                0L, List.of());
        private static final StreamCodec<ByteBuf, List<ChunkPos>> CHUNKS =
                ByteBufCodecs.VAR_LONG.map(ChunkPos::new, ChunkPos::toLong).apply(ByteBufCodecs.list());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC =
                StreamCodec.of(Section::encode, Section::decode);

        private static void encode(RegistryFriendlyByteBuf buffer, Section section) {
            Territory.STREAM_CODEC.encode(buffer, section.territory);
            for (BlockPos pos : new BlockPos[] {section.mountain, section.palace, section.entry}) {
                BlockPos.STREAM_CODEC.encode(buffer, pos);
            }
            Field.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.fields);
            CHUNKS.encode(buffer, section.planned);
            Pen.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.pastures);
            for (int value : new int[] {section.realmRadius, section.fieldCap, section.pastureCap}) {
                buffer.writeVarInt(value);
            }
            buffer.writeVarLong(section.mud);
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.animals);
        }

        private static Section decode(RegistryFriendlyByteBuf buffer) {
            Territory territory = Territory.STREAM_CODEC.decode(buffer);
            BlockPos mountain = BlockPos.STREAM_CODEC.decode(buffer);
            BlockPos palace = BlockPos.STREAM_CODEC.decode(buffer);
            BlockPos entry = BlockPos.STREAM_CODEC.decode(buffer);
            List<Field> fields = Field.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            List<ChunkPos> planned = CHUNKS.decode(buffer);
            List<Pen> pastures = Pen.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            int realmRadius = buffer.readVarInt();
            int fieldCap = buffer.readVarInt();
            int pastureCap = buffer.readVarInt();
            long mud = buffer.readVarLong();
            List<ResourceLocation> animals = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            return new Section(territory, mountain, realmRadius, palace, entry, fields, planned, pastures, fieldCap,
                    pastureCap, mud, animals);
        }

        public @NotNull Siting.Plot siting() {
            List<Long> taken = new ArrayList<>();
            for (Field field : fields) taken.add(ChunkPos.asLong(field.chunkX(), field.chunkZ()));
            for (ChunkPos plan : planned) taken.add(plan.toLong());
            List<Long> corners = new ArrayList<>();
            for (Pen pen : pastures) corners.add(ChunkPos.asLong(pen.chunkX(), pen.chunkZ()));
            return Siting.Plot.of(territory, mountain.getX(), mountain.getZ(), realmRadius, taken, corners);
        }

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    @Override
    public @NotNull Page page() { return Page.MAP; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() { return List.of(RevealChunk.TYPE); }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        LandSite site = BlissfulLandLookup.siteOf(level.getServer(), data.land());
        RealmSite realm = SecretRealms.siteOf(level.getServer(), data.land().realm());
        FarmState farm = FarmState.of(data);
        List<Section.Field> fields = new ArrayList<>();
        for (FieldPlot field : farm.fields()) {
            FarmWork.Count count = FarmWork.count(level, field);
            fields.add(new Section.Field(field.chunk().x, field.chunk().z, count.mature(), count.growing(),
                    farm.crop(field), farm.kind(field).ordinal()));
        }
        List<ChunkPos> planned = new ArrayList<>();
        for (long plan : farm.plans()) planned.add(new ChunkPos(plan));
        Map<ResourceLocation, List<Animal>> herds = LandCensus.take(level, HerdWork.CENSUS);
        TreeSet<ResourceLocation> animals = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        herds.forEach((type, herd) -> {
            if (!herd.isEmpty() && !PastureWork.isPredator(herd.getFirst())) animals.add(type);
        });
        PastureSettings settings = PastureSettings.of(data);
        List<Section.Pen> pastures = new ArrayList<>();
        for (ChunkPos corner : HerdState.of(data).pastures()) {
            PastureWork.Pasture pasture = PastureWork.Pasture.at(corner.x, corner.z);
            int penned = PastureWork.count(PastureWork.penned(herds, List.of(pasture)));
            List<ResourceLocation> excluded = List.copyOf(settings.excluded(corner));
            animals.addAll(excluded);
            pastures.add(new Section.Pen(corner.x, corner.z, penned, settings.autoGather(corner), excluded));
        }
        return new Section(data.territory(), site == null ? BlockPos.ZERO : site.center(),
                realm == null ? 0 : realm.radius(), site == null ? BlockPos.ZERO : site.palace(),
                site == null ? BlockPos.ZERO : site.entry(), fields, planned, pastures, Siting.FIELD_CAP,
                Siting.PASTURE_CAP, BlissfulStorage.of(data).count(FarmWork::isMud), List.copyOf(animals));
    }

    //region Siting [服务端复判]
    public static @NotNull Siting.Plot plot(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        RealmSite realm = SecretRealms.siteOf(level.getServer(), data.land().realm());
        List<Long> fields = new ArrayList<>();
        for (FieldPlot field : FarmState.of(data).fields()) fields.add(field.chunk().toLong());
        for (long plan : FarmState.of(data).plans()) fields.add(plan);
        List<Long> corners = new ArrayList<>();
        for (ChunkPos corner : HerdState.of(data).pastures()) corners.add(corner.toLong());
        return Siting.Plot.of(data.territory(), realm == null ? 0 : realm.center().getX(),
                realm == null ? 0 : realm.center().getZ(), realm == null ? 0 : realm.radius(), fields, corners);
    }
    //endregion
}
