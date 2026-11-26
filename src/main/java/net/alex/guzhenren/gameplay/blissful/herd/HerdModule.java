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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 「养殖」页模块：每种动物一行（数量、托管开关、保持数、库藏饲料），喂养繁殖、宰杀收取，以及养殖托管。
 * <ul>
 *   <li>行 = 清点到的种类，加上设过保持数或开着托管、眼下一只没有的种类；按实体 ID 排序。</li>
 *   <li>库藏饲料按该种第一只动物的 {@code isFood} 数库藏，一只没有的种类不知道吃什么，记 {@value #UNKNOWN_FEED}。</li>
 *   <li>{@link Section#slaughterPreview()} 是现在点「宰杀收取」会宰几头，规则同 {@link HerdWork#slaughter}。</li>
 *   <li>规则都在 {@link HerdWork}；托管每轮宰杀加繁殖至多 {@value #HOSTING_ANIMALS} 只，结果记进 {@link HerdState}，页
 *       面显示下一轮倒计时与上一轮结果。</li>
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

    public record Section(@NotNull List<Row> rows, int slaughterPreview, int hostingIn, int lastSlaughtered,
                          int lastBred) implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                Row.STREAM_CODEC.apply(ByteBufCodecs.list()), Section::rows,
                ByteBufCodecs.VAR_INT, Section::slaughterPreview,
                ByteBufCodecs.VAR_INT, Section::hostingIn,
                ByteBufCodecs.VAR_INT, Section::lastSlaughtered,
                ByteBufCodecs.VAR_INT, Section::lastBred,
                Section::new);

        public boolean hosting() { return rows.stream().anyMatch(Row::hosted); }

        @Override
        public @NotNull Page page() { return Page.HUSBANDRY; }
    }

    //region Actions [操作]
    public record Breed() implements PanelAction {

        public static final PanelAction.Type<Breed> TYPE = new PanelAction.Type<>(Breed.class,
                StreamCodec.unit(new Breed()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            HerdWork.Breed bred = HerdWork.breed(LandCensus.herds(context.level()), context.data().storage(),
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
                    LandCensus.herds(context.level()), data.herd().keep(), data.storage(), context.player(),
                    HerdWork.LIMIT));
        }
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
    public @NotNull Page page() { return Page.HUSBANDRY; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Breed.TYPE, Slaughter.TYPE, SetHosting.TYPE, SetKeep.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        HerdState state = data.herd();
        Map<ResourceLocation, List<Animal>> herds = LandCensus.herds(level);
        Map<ResourceLocation, List<Animal>> sorted = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
        sorted.putAll(herds);
        state.keep().keySet().forEach(type -> sorted.putIfAbsent(type, List.of()));
        state.hosted().forEach(type -> sorted.putIfAbsent(type, List.of()));
        List<Row> rows = new ArrayList<>();
        sorted.forEach((type, animals) -> {
            long feed = animals.isEmpty() ? UNKNOWN_FEED : data.storage().count(animals.getFirst()::isFood);
            rows.add(new Row(type, animals.size(), state.keep(type), state.isHosted(type), feed));
        });
        return new Section(rows, HerdWork.slaughterCount(herds, state.keep(), HerdWork.LIMIT),
                BlissfulLands.secondsToHosting(level.getServer()), state.lastSlaughtered(), state.lastBred());
    }

    @Override
    public int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        if (!data.herd().hosting()) return 0;

        HerdWork.Hosted hosted = HerdWork.host(level, LandCensus.herds(level), data, owner, HOSTING_ANIMALS);
        data.herd().setLastRound(hosted.slaughtered(), hosted.bred());
        return hosted.total();
    }
}
