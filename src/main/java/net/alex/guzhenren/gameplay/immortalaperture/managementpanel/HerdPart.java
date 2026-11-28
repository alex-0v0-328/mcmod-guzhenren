package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.alex.guzhenren.gameplay.immortalaperture.herd.GatherWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureSettings;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * 「养殖」小节面板段：每个牧场一行（指定动物、栏内头数、下限、现宰数、栏外同种头数、库藏饲料），牧场合计，喂养繁殖、宰杀
 * 收取；地图上的牧场操作（放置、迁移、拆除、收拢、指定动物、下限、单个牧场宰杀）也登记在这里。
 * <ul>
 *   <li>一个牧场只养指定的一种（Alex 定）；喂养、宰杀与托管只算牧场里这一种（{@link HerdWork#pens}）。</li>
 *   <li>{@link PlacePasture}、{@link MovePasture} 先按 {@link Siting#pasture} 复判（迁移排除自己原来的 4 区块），再圈地
 *       并挂强加载；迁移先拆旧栅栏、撤旧强加载，再把旧栏 4 区块里的可支使动物全部传进新栏；{@link RemovePasture} 只拆栅
 *       栏、撤强加载，动物留原地；{@link Gather} 经 {@link GatherWork} 把栏外的指定动物收进来（不设冷却）。牧场设置
 *       （{@link PastureSettings}）按西北角记：放置清掉同址旧设置，迁移随牧场搬走，拆除删掉。</li>
 *   <li>{@link #rows} 与 {@link #kinds} 也供地图页面板段下发（牧场对象栏与「指定动物」列表）。</li>
 *   <li>库藏饲料按畜群第一只的 {@code isFood} 数库藏，畜群为空不知道吃什么，记 {@value #UNKNOWN_FEED}。</li>
 *   <li>托管有主就跑（多出下限宰、不足下限喂），规则在 {@link HerdWork}；页面显示下一轮倒计时与上一轮结果。</li>
 *   <li>本文件有意略超 300 行：牧场七个操作要与养殖行同处一个面板段登记（面板按面板段编码操作），拆出去反而多一层转发。</li>
 * </ul>
 */

public final class HerdPart implements PanelSectionSource<HerdPart.Section> {

    public static final HerdPart INSTANCE = new HerdPart();
    public static final long UNKNOWN_FEED = -1L;
    private static final StreamCodec<ByteBuf, Optional<ResourceLocation>> ANIMAL =
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC);

    private HerdPart() {}

    /** 一个牧场的一行：{@code herd} 是栏内指定动物头数，{@code penned} 是栏内全部动物，{@code outside} 是栏外同种。 */
    public record Row(int index, int chunkX, int chunkZ, @NotNull Optional<ResourceLocation> animal, int penned,
                      int herd, int floor, int slaughter, int outside, long feed) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.of(
                (buffer, row) -> {
                    for (int value : new int[] {row.index, row.chunkX, row.chunkZ}) buffer.writeVarInt(value);
                    ANIMAL.encode(buffer, row.animal);
                    for (int value : new int[] {row.penned, row.herd, row.floor, row.slaughter, row.outside}) {
                        buffer.writeVarInt(value);
                    }
                    buffer.writeVarLong(row.feed);
                },
                buffer -> new Row(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), ANIMAL.decode(buffer),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarLong()));
    }

    /** 福地里一种可支使动物与它的头数（「指定动物」列表）。 */
    public record Kind(@NotNull ResourceLocation type, int count) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Kind> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Kind::type, ByteBufCodecs.VAR_INT, Kind::count, Kind::new);
    }

    public record Pen(int pastures, int inside, int outside) {

        public static final Pen EMPTY = new Pen(0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Pen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Pen::pastures,
                ByteBufCodecs.VAR_INT, Pen::inside,
                ByteBufCodecs.VAR_INT, Pen::outside,
                Pen::new);
    }

    public record Section(@NotNull List<Row> rows, @NotNull Pen pen, int hostingIn, int lastSlaughtered,
                          int lastBred) implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), Pen.EMPTY, 0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                Row.STREAM_CODEC.apply(ByteBufCodecs.list()), Section::rows,
                Pen.STREAM_CODEC, Section::pen,
                ByteBufCodecs.VAR_INT, Section::hostingIn,
                ByteBufCodecs.VAR_INT, Section::lastSlaughtered,
                ByteBufCodecs.VAR_INT, Section::lastBred,
                Section::new);

        public int slaughterPreview() { return Math.min(rows.stream().mapToInt(Row::slaughter).sum(), HerdRules.LIMIT); }

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Breed() implements PanelAction {

        public static final PanelAction.Type<Breed> TYPE = new PanelAction.Type<>(Breed.class,
                StreamCodec.unit(new Breed()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            HerdWork.Breed bred = HerdWork.breedAll(HerdWork.pens(context.level(), context.data()),
                    BlissfulStorage.of(context.data()), HerdRules.LIMIT);
            return LandNoticeEvent.text("bred", bred.animals(), bred.food());
        }
    }

    public record Slaughter() implements PanelAction {

        public static final PanelAction.Type<Slaughter> TYPE = new PanelAction.Type<>(Slaughter.class,
                StreamCodec.unit(new Slaughter()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            return LandNoticeEvent.text("slaughtered", HerdWork.slaughterAll(context.level(),
                    HerdWork.pens(context.level(), data), BlissfulStorage.of(data), context.player(), HerdRules.LIMIT));
        }
    }

    public record PlacePasture(int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<PlacePasture> TYPE = new PanelAction.Type<>(PlacePasture.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, PlacePasture::chunkX, ByteBufCodecs.VAR_INT,
                        PlacePasture::chunkZ, PlacePasture::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            Siting.Reason reason = Siting.pasture(MapPart.plot(level, data), Siting.loadedTiles(level,
                    data.territory()), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) return refused(reason);

            PastureWork.Pasture pasture = PastureWork.Pasture.at(chunkX, chunkZ);
            PastureWork.Enclosed done = PastureWork.enclose(level, pasture, context.player());
            if (done == null) return LandNoticeEvent.text("grief_denied");
            HerdState.of(data).addPasture(new ChunkPos(chunkX, chunkZ));
            PastureSettings.of(data).forget(new ChunkPos(chunkX, chunkZ));
            PastureWork.forceChunks(level, pasture, true);
            return enclosed(done);
        }
    }

    public record MovePasture(int index, int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<MovePasture> TYPE = new PanelAction.Type<>(MovePasture.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, MovePasture::index, ByteBufCodecs.VAR_INT,
                        MovePasture::chunkX, ByteBufCodecs.VAR_INT, MovePasture::chunkZ, MovePasture::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            ChunkPos old = HerdState.of(data).pasture(index);
            if (old == null) return LandNoticeEvent.text("no_pasture");
            Siting.Reason reason = Siting.pasture(MapPart.plot(level, data).without(old.x, old.z),
                    Siting.loadedTiles(level, data.territory()), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) return refused(reason);

            PastureWork.Pasture from = PastureWork.Pasture.at(old.x, old.z);
            PastureWork.Pasture to = PastureWork.Pasture.at(chunkX, chunkZ);
            if (PastureWork.unfence(level, from, context.player()) == null) {
                return LandNoticeEvent.text("grief_denied");
            }
            PastureWork.Enclosed done = PastureWork.enclose(level, to, context.player());
            if (done == null) return LandNoticeEvent.text("grief_denied");
            HerdState.of(data).setPasture(index, new ChunkPos(chunkX, chunkZ));
            PastureSettings.of(data).move(old, new ChunkPos(chunkX, chunkZ));
            PastureWork.forceChunks(level, to, true);
            int moved = PastureWork.relocate(level, from, to, level.getRandom(),
                    level.getEntitiesOfClass(Animal.class, from.box(level)));
            PastureWork.forceChunks(level, from, false);
            return LandNoticeEvent.text("pasture_moved", moved, done.placed());
        }
    }

    public record RemovePasture(int index) implements PanelAction {

        public static final PanelAction.Type<RemovePasture> TYPE = new PanelAction.Type<>(RemovePasture.class,
                ByteBufCodecs.VAR_INT.map(RemovePasture::new, RemovePasture::index));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            ChunkPos corner = HerdState.of(data).pasture(index);
            if (corner == null) return LandNoticeEvent.text("no_pasture");
            PastureWork.Pasture pasture = PastureWork.Pasture.at(corner.x, corner.z);
            Integer removed = PastureWork.unfence(level, pasture, context.player());
            if (removed == null) return LandNoticeEvent.text("grief_denied");
            PastureWork.forceChunks(level, pasture, false);
            HerdState.of(data).removePasture(index);
            PastureSettings.of(data).forget(corner);
            return LandNoticeEvent.text("pasture_removed", removed);
        }
    }

    public record Gather(int index) implements PanelAction {

        public static final PanelAction.Type<Gather> TYPE = new PanelAction.Type<>(Gather.class,
                ByteBufCodecs.VAR_INT.map(Gather::new, Gather::index));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            ChunkPos corner = HerdState.of(data).pasture(index);
            if (corner == null) return LandNoticeEvent.text("no_pasture");
            if (PastureSettings.of(data).animal(corner) == null) return LandNoticeEvent.text("no_pasture_animal");
            int moved = GatherWork.gather(level, data, corner, level.getRandom(), level.getAllEntities());
            return LandNoticeEvent.text("gathered", moved);
        }
    }

    public record SetAnimal(int index, @NotNull Optional<ResourceLocation> animal) implements PanelAction {

        public static final PanelAction.Type<SetAnimal> TYPE = new PanelAction.Type<>(SetAnimal.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetAnimal::index, ANIMAL, SetAnimal::animal,
                        SetAnimal::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ChunkPos corner = HerdState.of(context.data()).pasture(index);
            if (corner == null) return LandNoticeEvent.text("no_pasture");
            if (animal.isPresent() && !BuiltInRegistries.ENTITY_TYPE.containsKey(animal.get())) return Component.empty();
            PastureSettings.of(context.data()).setAnimal(corner, animal.orElse(null));
            return Component.empty();
        }
    }

    public record SetFloor(int index, int floor) implements PanelAction {

        public static final PanelAction.Type<SetFloor> TYPE = new PanelAction.Type<>(SetFloor.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetFloor::index, ByteBufCodecs.VAR_INT, SetFloor::floor,
                        SetFloor::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ChunkPos corner = HerdState.of(context.data()).pasture(index);
            if (corner == null) return LandNoticeEvent.text("no_pasture");
            PastureSettings.of(context.data()).setFloor(corner, floor);
            return Component.empty();
        }
    }

    public record SlaughterPen(int index) implements PanelAction {

        public static final PanelAction.Type<SlaughterPen> TYPE = new PanelAction.Type<>(SlaughterPen.class,
                ByteBufCodecs.VAR_INT.map(SlaughterPen::new, SlaughterPen::index));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            List<HerdWork.Pen> pens = HerdWork.pens(context.level(), data);
            if (index < 0 || index >= pens.size()) return LandNoticeEvent.text("no_pasture");
            HerdWork.Pen pen = pens.get(index);
            return LandNoticeEvent.text("slaughtered", HerdWork.slaughter(context.level(), pen.herd(), pen.floor(),
                    BlissfulStorage.of(data), context.player(), HerdRules.LIMIT));
        }
    }

    private static Component refused(Siting.Reason reason) {
        return LandNoticeEvent.text("siting_refused", Component.translatable(reason.key()));
    }

    private static Component enclosed(PastureWork.Enclosed done) {
        return done.blocked() > 0 ? LandNoticeEvent.text("enclosed_blocked", done.placed(), done.blocked())
                : LandNoticeEvent.text("enclosed", done.placed());
    }

    //endregion

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Breed.TYPE, Slaughter.TYPE, SlaughterPen.TYPE, SetAnimal.TYPE, SetFloor.TYPE, PlacePasture.TYPE,
                MovePasture.TYPE, RemovePasture.TYPE, Gather.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        Map<ResourceLocation, List<Animal>> census = LandCensus.take(level, HerdWork.CENSUS);
        List<Row> rows = rows(census, data);
        int inside = PastureWork.count(PastureWork.penned(census, PastureWork.pastures(data)));
        Pen pen = new Pen(rows.size(), inside, PastureWork.count(census) - inside);
        HerdState state = HerdState.of(data);
        return new Section(rows, pen, LandHostingJob.secondsUntilNext(level.getServer()), state.lastSlaughtered(),
                state.lastBred());
    }

    public static @NotNull List<Row> rows(@NotNull Map<ResourceLocation, List<Animal>> census,
                                          @NotNull BlissfulLandData data) {
        List<PastureWork.Pasture> pastures = PastureWork.pastures(data);
        List<Row> rows = new ArrayList<>();
        for (HerdWork.Pen pen : HerdWork.pens(census, data)) {
            ResourceLocation animal = pen.animal();
            List<Animal> all = animal == null ? List.of() : census.getOrDefault(animal, List.of());
            long feed = pen.herd().isEmpty() ? UNKNOWN_FEED : BlissfulStorage.of(data).count(pen.herd().getFirst()::isFood);
            rows.add(new Row(pen.index(), pen.corner().x, pen.corner().z, Optional.ofNullable(animal), pen.penned(),
                    pen.herd().size(), pen.floor(), HerdWork.slaughterCount(pen.herd(), pen.floor()),
                    Math.min(GatherWork.outside(all, pastures), HerdRules.GATHER_LIMIT), feed));
        }
        return rows;
    }

    public static @NotNull List<Kind> kinds(@NotNull Map<ResourceLocation, List<Animal>> census) {
        List<Kind> kinds = new ArrayList<>();
        new TreeMap<>(census).forEach((type, herd) -> {
            if (!herd.isEmpty() && !PastureWork.isPredator(herd.getFirst())) kinds.add(new Kind(type, herd.size()));
        });
        return kinds;
    }
}
