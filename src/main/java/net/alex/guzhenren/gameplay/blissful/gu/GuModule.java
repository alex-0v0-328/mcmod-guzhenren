package net.alex.guzhenren.gameplay.blissful.gu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.land.LandCensus;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 「蛊虫」页模块：福地里每种野生蛊一行（捕获后的蛊虫物品图标、数量、托管开关、保持数），「捕获入库」，以及蛊虫托管。
 * <ul>
 *   <li>行 = 清点到的种类，加上设过保持数或开着托管、眼下一只没有的种类；按实体 ID 排序。一只没有的种类不知道捕到什么物
 *       品，图标记 {@code minecraft:air}，页面不画。</li>
 *   <li>{@link Section#capturePreview()} 是现在点「捕获入库」会捕几只，规则同 {@link GuWork#capture}。</li>
 *   <li>规则都在 {@link GuWork}；托管每轮至多 {@value #HOSTING_GU} 只（单独预算，不占养殖、种植的），结果记进
 *       {@link GuState}，页面显示下一轮倒计时与上一轮捕获数。</li>
 * </ul>
 */

public final class GuModule implements LandModule<GuModule.Section> {

    public static final GuModule INSTANCE = new GuModule();
    public static final int HOSTING_GU = 16;

    private GuModule() {}

    public record Row(@NotNull ResourceLocation type, @NotNull ResourceLocation item, int count, int keep,
                      boolean hosted) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Row::type,
                ResourceLocation.STREAM_CODEC, Row::item,
                ByteBufCodecs.VAR_INT, Row::count,
                ByteBufCodecs.VAR_INT, Row::keep,
                ByteBufCodecs.BOOL, Row::hosted,
                Row::new);
    }

    public record Section(@NotNull List<Row> rows, int capturePreview, int hostingIn, int lastCaptured)
            implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                Row.STREAM_CODEC.apply(ByteBufCodecs.list()), Section::rows,
                ByteBufCodecs.VAR_INT, Section::capturePreview,
                ByteBufCodecs.VAR_INT, Section::hostingIn,
                ByteBufCodecs.VAR_INT, Section::lastCaptured,
                Section::new);

        public boolean hosting() { return rows.stream().anyMatch(Row::hosted); }

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Capture() implements PanelAction {

        public static final PanelAction.Type<Capture> TYPE = new PanelAction.Type<>(Capture.class,
                StreamCodec.unit(new Capture()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            return EarthSpiritPanel.notice("captured", GuWork.capture(LandCensus.gus(context.level()),
                    data.gu().keep(), data.storage(), GuWork.LIMIT));
        }
    }

    public record SetHosting(@NotNull ResourceLocation type, boolean on) implements PanelAction {

        public static final PanelAction.Type<SetHosting> TYPE = new PanelAction.Type<>(SetHosting.class,
                StreamCodec.composite(ResourceLocation.STREAM_CODEC, SetHosting::type, ByteBufCodecs.BOOL,
                        SetHosting::on, SetHosting::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (isWildGu(type)) context.data().gu().setHosted(type, on);
            return Component.empty();
        }
    }

    public record SetKeep(@NotNull ResourceLocation type, int keep) implements PanelAction {

        public static final PanelAction.Type<SetKeep> TYPE = new PanelAction.Type<>(SetKeep.class,
                StreamCodec.composite(ResourceLocation.STREAM_CODEC, SetKeep::type, ByteBufCodecs.VAR_INT,
                        SetKeep::keep, SetKeep::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (isWildGu(type)) context.data().gu().setKeep(type, keep);
            return Component.empty();
        }
    }
    //endregion

    private static boolean isWildGu(ResourceLocation type) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(type)
                .map(entity -> entity.is(ModEntityTypeTags.WILD_GU))
                .orElse(false);
    }

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Capture.TYPE, SetHosting.TYPE, SetKeep.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        GuState state = context.data().gu();
        Map<ResourceLocation, List<Mob>> swarms = LandCensus.gus(level);
        Map<ResourceLocation, List<Mob>> sorted = new TreeMap<>(
                Comparator.comparing(ResourceLocation::toString));
        sorted.putAll(swarms);
        state.keep().keySet().forEach(type -> sorted.putIfAbsent(type, List.of()));
        state.hosted().forEach(type -> sorted.putIfAbsent(type, List.of()));
        ResourceLocation none = BuiltInRegistries.ITEM.getKey(Items.AIR);
        List<Row> rows = new ArrayList<>();
        sorted.forEach((type, gus) -> {
            ResourceLocation item = gus.isEmpty() ? none
                    : BuiltInRegistries.ITEM.getKey(GuWork.caughtStack(gus.getFirst()).getItem());
            rows.add(new Row(type, item, gus.size(), state.keep(type), state.isHosted(type)));
        });
        return new Section(rows, GuWork.captureCount(swarms, state.keep(), GuWork.LIMIT),
                BlissfulLands.secondsToHosting(level.getServer()), state.lastCaptured());
    }

    @Override
    public int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        if (!data.gu().hosting()) return 0;

        int captured = GuWork.host(LandCensus.gus(level), data, HOSTING_GU);
        data.gu().setLastCaptured(captured);
        return captured;
    }
}
