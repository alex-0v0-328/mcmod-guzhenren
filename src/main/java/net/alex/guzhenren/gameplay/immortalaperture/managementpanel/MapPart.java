package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.LandMaps;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSite;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.immortalaperture.territory.LandTeleport;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 「地图」页面板段：下发地图页叠加层与卡片需要的经营数据（疆域、山心与秘境半径、行宫、入口、田与其熟 / 未熟株数、生长
 * 进度、作物田型、规划、牧场（{@link HerdPart#rows}）、选址上限、库藏腐泥冻土数、骨粉数与各作物种子数（与
 * {@link FarmRules#crops()} 同序）、福地里可支使动物的种类与头数（{@link HerdPart#kinds}））；
 * 地形片与活物标记另走地图载荷，不在这一段。选址操作在 {@code FarmPart}、{@code HerdPart}；本段只有「探明」
 * （{@link RevealChunk}，拿着选址工具点迷雾，交 {@link LandMaps#reveal} 限流后生成并加载）与地图右键传送：
 * {@link Teleport} 只许落在疆域内已探明的区块（{@link LandMaps#isKnown}），落点由 {@link LandTeleport#landing} 找，传送
 * 后面板置漫游不再按距离关（「回到地灵身边」已删，Alex 定）。
 * <ul>
 *   <li>秘境未解出时山心、行宫、入口都是 {@link BlockPos#ZERO}、秘境半径 0（{@link Siting} 视作不查秘境）。</li>
 *   <li>{@link #plot}（服务端复判）与 {@link Section#siting}（客户端预判）取同一组占地：田、规划、牧场 4 区块；服务端
 *       地形读 {@link Siting#loadedTiles}。</li>
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

    public record Teleport(int x, int z) implements PanelAction {

        public static final PanelAction.Type<Teleport> TYPE = new PanelAction.Type<>(Teleport.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Teleport::x, ByteBufCodecs.VAR_INT, Teleport::z,
                        Teleport::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            if (!data.territory().contains(chunkX, chunkZ) || !LandMaps.isKnown(data.land(), chunkX, chunkZ)
                    && context.level().getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                return LandNoticeEvent.text("teleport_fog");
            }
            Vec3 spot = LandTeleport.landing(context.level(), x, z);
            if (spot == null) return LandNoticeEvent.text("teleport_no_footing", LandTeleport.SEARCH);
            context.menu().setRoaming();
            LandTeleport.go(context.player(), context.level(), spot);
            return LandNoticeEvent.text("teleported", Mth.floor(spot.x), Mth.floor(spot.y), Mth.floor(spot.z));
        }
    }

    public record Section(@NotNull Territory territory, @NotNull BlockPos mountain, int realmRadius,
                          @NotNull BlockPos palace, @NotNull BlockPos entry, @NotNull List<Field> fields,
                          @NotNull List<ChunkPos> planned, @NotNull List<HerdPart.Row> pastures, int fieldCap,
                          int pastureCap, long mud, long boneMeal, @NotNull List<HerdPart.Kind> animals,
                          @NotNull List<Long> seeds)
            implements PanelSection {

        public record Field(int chunkX, int chunkZ, int mature, int growing, int growth, int crop, int kind,
                            int main) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Field> STREAM_CODEC = StreamCodec.of(
                    (buffer, field) -> {
                        for (int value : new int[] {field.chunkX, field.chunkZ, field.mature, field.growing,
                                field.growth, field.crop, field.kind, field.main}) {
                            buffer.writeVarInt(value);
                        }
                    },
                    buffer -> new Field(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarInt()));

            public int planted() { return mature + growing; }
        }

        public static final Section EMPTY = new Section(TerritoryPart.Section.EMPTY.territory(), BlockPos.ZERO, 0,
                BlockPos.ZERO, BlockPos.ZERO, List.of(), List.of(), List.of(), Siting.FIELD_CAP, Siting.PASTURE_CAP,
                0L, 0L, List.of(), List.of());
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
            HerdPart.Row.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.pastures);
            for (int value : new int[] {section.realmRadius, section.fieldCap, section.pastureCap}) {
                buffer.writeVarInt(value);
            }
            buffer.writeVarLong(section.mud);
            buffer.writeVarLong(section.boneMeal);
            HerdPart.Kind.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.animals);
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(buffer, section.seeds);
        }

        private static Section decode(RegistryFriendlyByteBuf buffer) {
            Territory territory = Territory.STREAM_CODEC.decode(buffer);
            BlockPos mountain = BlockPos.STREAM_CODEC.decode(buffer);
            BlockPos palace = BlockPos.STREAM_CODEC.decode(buffer);
            BlockPos entry = BlockPos.STREAM_CODEC.decode(buffer);
            List<Field> fields = Field.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            List<ChunkPos> planned = CHUNKS.decode(buffer);
            List<HerdPart.Row> pastures = HerdPart.Row.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            int realmRadius = buffer.readVarInt();
            int fieldCap = buffer.readVarInt();
            int pastureCap = buffer.readVarInt();
            long mud = buffer.readVarLong();
            long boneMeal = buffer.readVarLong();
            List<HerdPart.Kind> animals = HerdPart.Kind.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            List<Long> seeds = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(buffer);
            return new Section(territory, mountain, realmRadius, palace, entry, fields, planned, pastures, fieldCap,
                    pastureCap, mud, boneMeal, animals, seeds);
        }

        public long seedsOf(int crop) { return crop >= 0 && crop < seeds.size() ? seeds.get(crop) : 0L; }

        public @NotNull Siting.Plot siting() {
            List<Long> taken = new ArrayList<>();
            for (Field field : fields) taken.add(ChunkPos.asLong(field.chunkX(), field.chunkZ()));
            for (ChunkPos plan : planned) taken.add(plan.toLong());
            List<Long> corners = new ArrayList<>();
            for (HerdPart.Row pen : pastures) corners.add(ChunkPos.asLong(pen.chunkX(), pen.chunkZ()));
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
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(RevealChunk.TYPE, Teleport.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        LandSite site = BlissfulLandLookup.siteOf(level.getServer(), data.land());
        RealmSite realm = SecretRealms.siteOf(level.getServer(), data.land().realm());
        FarmState farm = FarmState.of(data);
        List<Section.Field> fields = new ArrayList<>();
        for (FieldPlot field : farm.fields()) {
            FarmWork.Look look = FarmWork.look(level, field);
            fields.add(new Section.Field(field.chunk().x, field.chunk().z, look.mature(), look.growing(), look.growth(),
                    farm.crop(field), farm.kind(field).ordinal(), look.main()));
        }
        List<ChunkPos> planned = new ArrayList<>();
        for (long plan : farm.plans()) planned.add(new ChunkPos(plan));
        Map<ResourceLocation, List<Animal>> herds = LandCensus.take(level, HerdWork.CENSUS);
        return new Section(data.territory(), site == null ? BlockPos.ZERO : site.center(),
                realm == null ? 0 : realm.radius(), site == null ? BlockPos.ZERO : site.palace(),
                site == null ? BlockPos.ZERO : site.entry(), fields, planned, HerdPart.rows(herds, data), Siting.FIELD_CAP,
                Siting.PASTURE_CAP, BlissfulStorage.of(data).count(FarmWork::isMud),
                BlissfulStorage.of(data).count(FarmWork::isBoneMeal), HerdPart.kinds(herds), seeds(data));
    }

    private static List<Long> seeds(BlissfulLandData data) {
        List<Long> seeds = new ArrayList<>();
        for (int crop = 0; crop < FarmRules.crops().size(); crop++) {
            seeds.add(BlissfulStorage.of(data).count(FarmWork.seedFilter(crop)));
        }
        return seeds;
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
