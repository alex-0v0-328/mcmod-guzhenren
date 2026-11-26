package net.alex.guzhenren.gameplay.blissful.herd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.LandCensus;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.map.MapModule;
import net.alex.guzhenren.gameplay.blissful.map.Siting;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 「养殖」小节模块：每种动物一行（数量、托管开关、保持数、库藏饲料），牧场一行（牧场数、栏内栏外），喂养繁殖、宰杀收
 * 取，以及养殖托管；地图上的牧场操作（放置、迁移、拆除、收拢）也登记在这里。
 * <ul>
 *   <li>行、喂养、宰杀与托管只算各牧场栏内的动物（{@link PastureWork#penned}）；栏外的只在牧场行里计数。</li>
 *   <li>{@link PlacePasture}、{@link MovePasture} 先按 {@link Siting#pasture} 复判（迁移排除自己原来的 4 区块），再圈地
 *       并挂强加载；迁移先拆旧栅栏、撤旧强加载，再把旧栏 4 区块里的可支使动物全部传进新栏；{@link RemovePasture} 只拆栅
 *       栏、撤强加载，动物留原地；{@link Gather} 收拢进指定牧场。</li>
 *   <li>行 = 栏内清点到的种类，加上设过保持数或开着托管、眼下一只没有的种类；按实体 ID 排序。</li>
 *   <li>库藏饲料按该种第一只动物的 {@code isFood} 数库藏，一只没有的种类不知道吃什么，记 {@value #UNKNOWN_FEED}。</li>
 *   <li>{@link Section#slaughterPreview()} 是现在点「宰杀收取」会宰几头，规则同 {@link HerdWork#slaughter}。</li>
 *   <li>规则都在 {@link HerdWork}；托管每轮宰杀加繁殖至多 {@value #HOSTING_ANIMALS} 只，结果记进 {@link HerdState}，页
 *       面显示下一轮倒计时与上一轮结果。</li>
 *   <li>本文件有意略超 300 行：牧场四个操作要与养殖行同处一个模块登记（面板按模块编码操作），拆出去反而多一层转发。
 *       </li>
 * </ul>
 */

public final class HerdModule implements LandModule<HerdModule.Section> {

    public static final HerdModule INSTANCE = new HerdModule();
    public static final int HOSTING_ANIMALS = 32;
    public static final long UNKNOWN_FEED = -1L;

    private HerdModule() {}

    public record Row(@NotNull ResourceLocation type, int count, int keep, boolean hosted, long feed) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Row::type,
                ByteBufCodecs.VAR_INT, Row::count,
                ByteBufCodecs.VAR_INT, Row::keep,
                ByteBufCodecs.BOOL, Row::hosted,
                ByteBufCodecs.VAR_LONG, Row::feed,
                Row::new);
    }

    public record Pen(int pastures, int inside, int outside) {

        public static final Pen EMPTY = new Pen(0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Pen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Pen::pastures,
                ByteBufCodecs.VAR_INT, Pen::inside,
                ByteBufCodecs.VAR_INT, Pen::outside,
                Pen::new);
    }

    public record Section(@NotNull List<Row> rows, @NotNull Pen pen, int slaughterPreview, int hostingIn,
                          int lastSlaughtered, int lastBred) implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), Pen.EMPTY, 0, 0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                Row.STREAM_CODEC.apply(ByteBufCodecs.list()), Section::rows,
                Pen.STREAM_CODEC, Section::pen,
                ByteBufCodecs.VAR_INT, Section::slaughterPreview,
                ByteBufCodecs.VAR_INT, Section::hostingIn,
                ByteBufCodecs.VAR_INT, Section::lastSlaughtered,
                ByteBufCodecs.VAR_INT, Section::lastBred,
                Section::new);

        public boolean hosting() { return rows.stream().anyMatch(Row::hosted); }

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Breed() implements PanelAction {

        public static final PanelAction.Type<Breed> TYPE = new PanelAction.Type<>(Breed.class,
                StreamCodec.unit(new Breed()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            HerdWork.Breed bred = HerdWork.breed(penned(context.level(), context.data()), context.data().storage(),
                    HerdWork.LIMIT, null);
            return EarthSpiritPanel.notice("bred", bred.animals(), bred.food());
        }
    }

    public record Slaughter() implements PanelAction {

        public static final PanelAction.Type<Slaughter> TYPE = new PanelAction.Type<>(Slaughter.class,
                StreamCodec.unit(new Slaughter()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            return EarthSpiritPanel.notice("slaughtered", HerdWork.slaughter(context.level(),
                    penned(context.level(), data), data.herd().keep(), data.storage(), context.player(),
                    HerdWork.LIMIT));
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
            Siting.Reason reason = Siting.pasture(MapModule.plot(level, data), MapModule.tiles(level,
                    data.territory()), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) return refused(reason);

            PastureWork.Pasture pasture = PastureWork.Pasture.at(chunkX, chunkZ);
            PastureWork.Enclosed done = PastureWork.enclose(level, pasture, context.player());
            if (done == null) return EarthSpiritPanel.notice("grief_denied");
            data.herd().addPasture(new ChunkPos(chunkX, chunkZ));
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
            ChunkPos old = data.herd().pasture(index);
            if (old == null) return EarthSpiritPanel.notice("no_pasture");
            Siting.Reason reason = Siting.pasture(MapModule.plot(level, data).without(old.x, old.z),
                    MapModule.tiles(level, data.territory()), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) return refused(reason);

            PastureWork.Pasture from = PastureWork.Pasture.at(old.x, old.z);
            PastureWork.Pasture to = PastureWork.Pasture.at(chunkX, chunkZ);
            if (PastureWork.unfence(level, from, context.player()) == null) {
                return EarthSpiritPanel.notice("grief_denied");
            }
            PastureWork.Enclosed done = PastureWork.enclose(level, to, context.player());
            if (done == null) return EarthSpiritPanel.notice("grief_denied");
            data.herd().setPasture(index, new ChunkPos(chunkX, chunkZ));
            PastureWork.forceChunks(level, to, true);
            int moved = PastureWork.relocate(level, from, to, level.getRandom(),
                    level.getEntitiesOfClass(Animal.class, from.box(level)));
            PastureWork.forceChunks(level, from, false);
            return EarthSpiritPanel.notice("pasture_moved", moved, done.placed());
        }
    }

    public record RemovePasture(int index) implements PanelAction {

        public static final PanelAction.Type<RemovePasture> TYPE = new PanelAction.Type<>(RemovePasture.class,
                ByteBufCodecs.VAR_INT.map(RemovePasture::new, RemovePasture::index));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            ChunkPos corner = data.herd().pasture(index);
            if (corner == null) return EarthSpiritPanel.notice("no_pasture");
            PastureWork.Pasture pasture = PastureWork.Pasture.at(corner.x, corner.z);
            Integer removed = PastureWork.unfence(level, pasture, context.player());
            if (removed == null) return EarthSpiritPanel.notice("grief_denied");
            PastureWork.forceChunks(level, pasture, false);
            data.herd().removePasture(index);
            return EarthSpiritPanel.notice("pasture_removed", removed);
        }
    }

    public record Gather(int index) implements PanelAction {

        public static final PanelAction.Type<Gather> TYPE = new PanelAction.Type<>(Gather.class,
                ByteBufCodecs.VAR_INT.map(Gather::new, Gather::index));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            ChunkPos corner = data.herd().pasture(index);
            if (corner == null) return EarthSpiritPanel.notice("no_pasture");
            long cooldown = PastureWork.gatherCooldown(level, data);
            if (cooldown > 0) return EarthSpiritPanel.notice("cooldown", EarthSpiritPanel.seconds(cooldown));

            int moved = PastureWork.gather(level, data, PastureWork.Pasture.at(corner.x, corner.z), level.getRandom(),
                    level.getAllEntities());
            return EarthSpiritPanel.notice("gathered", moved);
        }
    }

    private static Component refused(Siting.Reason reason) {
        return EarthSpiritPanel.notice("siting_refused", Component.translatable(reason.key()));
    }

    private static Component enclosed(PastureWork.Enclosed done) {
        return done.blocked() > 0 ? EarthSpiritPanel.notice("enclosed_blocked", done.placed(), done.blocked())
                : EarthSpiritPanel.notice("enclosed", done.placed());
    }

    public record SetHosting(@NotNull ResourceLocation type, boolean on) implements PanelAction {

        public static final PanelAction.Type<SetHosting> TYPE = new PanelAction.Type<>(SetHosting.class,
                StreamCodec.composite(ResourceLocation.STREAM_CODEC, SetHosting::type, ByteBufCodecs.BOOL,
                        SetHosting::on, SetHosting::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(type)) context.data().herd().setHosted(type, on);
            return Component.empty();
        }
    }

    public record SetKeep(@NotNull ResourceLocation type, int keep) implements PanelAction {

        public static final PanelAction.Type<SetKeep> TYPE = new PanelAction.Type<>(SetKeep.class,
                StreamCodec.composite(ResourceLocation.STREAM_CODEC, SetKeep::type, ByteBufCodecs.VAR_INT,
                        SetKeep::keep, SetKeep::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(type)) context.data().herd().setKeep(type, keep);
            return Component.empty();
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Breed.TYPE, Slaughter.TYPE, SetHosting.TYPE, SetKeep.TYPE, PlacePasture.TYPE, MovePasture.TYPE,
                RemovePasture.TYPE, Gather.TYPE);
    }

    private static Map<ResourceLocation, List<Animal>> penned(ServerLevel level, BlissfulLandData data) {
        return PastureWork.penned(LandCensus.herds(level), PastureWork.pastures(data));
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        HerdState state = data.herd();
        Map<ResourceLocation, List<Animal>> herds = penned(level, data);
        int inside = PastureWork.count(herds);
        Pen pen = new Pen(state.pastures().size(), inside, PastureWork.count(LandCensus.herds(level)) - inside);
        Map<ResourceLocation, List<Animal>> sorted = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
        sorted.putAll(herds);
        state.keep().keySet().forEach(type -> sorted.putIfAbsent(type, List.of()));
        state.hosted().forEach(type -> sorted.putIfAbsent(type, List.of()));
        List<Row> rows = new ArrayList<>();
        sorted.forEach((type, animals) -> {
            long feed = animals.isEmpty() ? UNKNOWN_FEED : data.storage().count(animals.getFirst()::isFood);
            rows.add(new Row(type, animals.size(), state.keep(type), state.isHosted(type), feed));
        });
        return new Section(rows, pen, HerdWork.slaughterCount(herds, state.keep(), HerdWork.LIMIT),
                BlissfulLands.secondsToHosting(level.getServer()), state.lastSlaughtered(), state.lastBred());
    }

    @Override
    public int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        if (!data.herd().hosting()) return 0;

        HerdWork.Hosted hosted = HerdWork.host(level, penned(level, data), data, owner, HOSTING_ANIMALS);
        data.herd().setLastRound(hosted.slaughtered(), hosted.bred());
        return hosted.total();
    }
}
