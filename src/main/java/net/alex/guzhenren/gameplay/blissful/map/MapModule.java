package net.alex.guzhenren.gameplay.blissful.map;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.LandCensus;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.LandSite;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.blissful.farm.FieldPlot;
import net.alex.guzhenren.gameplay.blissful.herd.PastureWork;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.alex.guzhenren.gameplay.blissful.territory.TerritoryModule;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

/**
 * 「地图」页模块：下发地图页叠加层需要的经营数据（疆域、山心与秘境半径、行宫、入口、田、规划、牧场、选址上限与冷却）；
 * 地形片与活物标记另走地图载荷，不在这一段。选址操作在 {@code FarmModule}、{@code HerdModule}。
 * <ul>
 *   <li>秘境未解出时山心、行宫、入口都是 {@link BlockPos#ZERO}、秘境半径 0（{@link Siting} 视作不查秘境）。</li>
 *   <li>{@link #plot} 与客户端的 {@link Siting.Plot#of(Section)} 取同一组占地：田、规划、牧场 4 区块。</li>
 *   <li>{@link #tiles}：服务端复判用，同步加载所需区块（只会是疆域内、客户端看过的区块）并用 {@link MapScanner#tile}
 *       现扫，同一次复判内缓存。</li>
 * </ul>
 */

public final class MapModule implements LandModule<MapModule.Section> {

    public static final MapModule INSTANCE = new MapModule();

    private MapModule() {}

    public record Section(@NotNull Territory territory, @NotNull BlockPos mountain, int realmRadius,
                          @NotNull BlockPos palace, @NotNull BlockPos entry, @NotNull List<Field> fields,
                          @NotNull List<ChunkPos> planned, @NotNull List<Pen> pastures, int fieldCap, int pastureCap,
                          int reclaimSeconds, int gatherSeconds) implements PanelSection {

        public record Field(int chunkX, int chunkZ, int mature, int growing) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Field> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Field::chunkX,
                    ByteBufCodecs.VAR_INT, Field::chunkZ,
                    ByteBufCodecs.VAR_INT, Field::mature,
                    ByteBufCodecs.VAR_INT, Field::growing,
                    Field::new);
        }

        public record Pen(int chunkX, int chunkZ, int penned) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Pen> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Pen::chunkX,
                    ByteBufCodecs.VAR_INT, Pen::chunkZ,
                    ByteBufCodecs.VAR_INT, Pen::penned,
                    Pen::new);
        }

        public static final Section EMPTY = new Section(TerritoryModule.Section.EMPTY.territory(), BlockPos.ZERO, 0,
                BlockPos.ZERO, BlockPos.ZERO, List.of(), List.of(), List.of(), Siting.FIELD_CAP, Siting.PASTURE_CAP,
                0, 0);
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
            for (int value : new int[] {section.realmRadius, section.fieldCap, section.pastureCap,
                    section.reclaimSeconds, section.gatherSeconds}) {
                buffer.writeVarInt(value);
            }
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
            int reclaimSeconds = buffer.readVarInt();
            int gatherSeconds = buffer.readVarInt();
            return new Section(territory, mountain, realmRadius, palace, entry, fields, planned, pastures, fieldCap,
                    pastureCap, reclaimSeconds, gatherSeconds);
        }

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    @Override
    public @NotNull Page page() { return Page.MAP; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() { return List.of(); }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        LandSite site = BlissfulLands.siteOf(level.getServer(), data.land());
        RealmSite realm = SecretRealms.siteOf(level.getServer(), data.land().realm());
        List<Section.Field> fields = new ArrayList<>();
        for (FieldPlot field : data.farm().fields()) {
            FarmWork.Count count = FarmWork.count(level, field);
            fields.add(new Section.Field(field.chunk().x, field.chunk().z, count.mature(), count.growing()));
        }
        List<ChunkPos> planned = new ArrayList<>();
        for (long plan : data.farm().plans()) planned.add(new ChunkPos(plan));
        List<Section.Pen> pastures = new ArrayList<>();
        for (ChunkPos corner : data.herd().pastures()) {
            PastureWork.Pasture pasture = PastureWork.Pasture.at(corner.x, corner.z);
            int penned = PastureWork.count(PastureWork.penned(LandCensus.herds(level), List.of(pasture)));
            pastures.add(new Section.Pen(corner.x, corner.z, penned));
        }
        return new Section(data.territory(), site == null ? BlockPos.ZERO : site.center(),
                realm == null ? 0 : realm.radius(), site == null ? BlockPos.ZERO : site.palace(),
                site == null ? BlockPos.ZERO : site.entry(), fields, planned, pastures, Siting.FIELD_CAP,
                Siting.PASTURE_CAP, Ticks.ceilSeconds(FarmWork.reclaimCooldown(level, data)),
                Ticks.ceilSeconds(PastureWork.gatherCooldown(level, data)));
    }

    //region Siting [服务端复判]
    public static @NotNull Siting.Plot plot(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        RealmSite realm = SecretRealms.siteOf(level.getServer(), data.land().realm());
        List<Long> fields = new ArrayList<>();
        for (FieldPlot field : data.farm().fields()) fields.add(field.chunk().toLong());
        for (long plan : data.farm().plans()) fields.add(plan);
        List<Long> corners = new ArrayList<>();
        for (ChunkPos corner : data.herd().pastures()) corners.add(corner.toLong());
        return Siting.Plot.of(data.territory(), realm == null ? 0 : realm.center().getX(),
                realm == null ? 0 : realm.center().getZ(), realm == null ? 0 : realm.radius(), fields, corners);
    }

    public static @NotNull Siting.Tiles tiles(@NotNull ServerLevel level, @NotNull Territory territory) {
        Long2ObjectOpenHashMap<MapTile> cache = new Long2ObjectOpenHashMap<>();
        return (chunkX, chunkZ) -> cache.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            return MapScanner.tile(territory, chunk);
        });
    }
    //endregion
}
