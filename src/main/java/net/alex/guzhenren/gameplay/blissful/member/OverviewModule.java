package net.alex.guzhenren.gameplay.blissful.member;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.LandCensus;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 「总览」页模块：亩数、胆石、动物、作物四个数，在场玩家（白名单开关、驱逐），「驱逐外人」。
 * <ul>
 *   <li>动物、作物取 {@link LandCensus} 的同刻清点；作物数 = 成熟 + 未熟。</li>
 *   <li>白名单与荡魂山页是同一份；规则在 {@link MemberService}。</li>
 * </ul>
 */

public final class OverviewModule implements LandModule<OverviewModule.Section> {

    public static final OverviewModule INSTANCE = new OverviewModule();

    private OverviewModule() {}

    public record Section(long acres, int gutsStones, int gutsCap, int animals, int crops,
                          @NotNull List<Person> players) implements PanelSection {

        public static final Section EMPTY = new Section(0L, 0, 0, 0, 0, List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Section::acres,
                ByteBufCodecs.VAR_INT, Section::gutsStones,
                ByteBufCodecs.VAR_INT, Section::gutsCap,
                ByteBufCodecs.VAR_INT, Section::animals,
                ByteBufCodecs.VAR_INT, Section::crops,
                Person.LIST_CODEC, Section::players,
                Section::new);

        @Override
        public @NotNull Page page() { return Page.OVERVIEW; }
    }

    //region Actions [操作]
    public record Expel(@NotNull UUID target) implements PanelAction {

        public static final PanelAction.Type<Expel> TYPE = new PanelAction.Type<>(Expel.class,
                UUIDUtil.STREAM_CODEC.map(Expel::new, Expel::target));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return MemberService.expel(context.level(), context.data(), target)
                    ? EarthSpiritPanel.notice("expelled") : EarthSpiritPanel.notice("expel_failed");
        }
    }

    public record ExpelOutsiders() implements PanelAction {

        public static final PanelAction.Type<ExpelOutsiders> TYPE = new PanelAction.Type<>(ExpelOutsiders.class,
                StreamCodec.unit(new ExpelOutsiders()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return EarthSpiritPanel.notice("expelled_outsiders",
                    MemberService.expelOutsiders(context.level(), context.data()));
        }
    }

    public record SetWhitelisted(@NotNull UUID target, boolean listed) implements PanelAction {

        public static final PanelAction.Type<SetWhitelisted> TYPE = new PanelAction.Type<>(SetWhitelisted.class,
                StreamCodec.composite(UUIDUtil.STREAM_CODEC, SetWhitelisted::target, ByteBufCodecs.BOOL,
                        SetWhitelisted::listed, SetWhitelisted::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return MemberService.setWhitelisted(context.player().server, context.data(), target, listed)
                    ? Component.empty() : EarthSpiritPanel.notice("player_offline");
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.OVERVIEW; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Expel.TYPE, ExpelOutsiders.TYPE, SetWhitelisted.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        int animals = 0;
        for (var herd : LandCensus.herds(level).values()) animals += herd.size();
        int crops = 0;
        for (FarmWork.Count count : LandCensus.crops(level, data).values()) crops += count.mature() + count.growing();
        return new Section(data.territory().acres(), GutsStones.get(level).total(), SoulsunderMountain.GUTS_STONE_CAP,
                animals, crops, present(level, data));
    }

    private static List<Person> present(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        List<Person> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            players.add(new Person(id, player.getGameProfile().getName(), data.isWhitelisted(id), data.isOwner(id)));
        }
        return players;
    }
}
