package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandMembership;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
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
 * 「总览」页面板段：亩数、胆石、动物、作物四个数，在场玩家（白名单开关、驱逐），「驱逐外人」。
 * <ul>
 *   <li>动物、作物取 {@link LandCensus} 的同刻清点；作物数 = 成熟 + 未熟。</li>
 *   <li>白名单与荡魂山页是同一份；规则在 {@link LandMembership}。</li>
 * </ul>
 */

public final class OverviewPart implements PanelSectionSource<OverviewPart.Section> {

    public static final OverviewPart INSTANCE = new OverviewPart();

    private OverviewPart() {}

    public record Section(long acres, int gutsStones, int gutsCap, int animals, int crops,
                          @NotNull List<WhitelistEntry> players) implements PanelSection {

        public static final Section EMPTY = new Section(0L, 0, 0, 0, 0, List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Section::acres,
                ByteBufCodecs.VAR_INT, Section::gutsStones,
                ByteBufCodecs.VAR_INT, Section::gutsCap,
                ByteBufCodecs.VAR_INT, Section::animals,
                ByteBufCodecs.VAR_INT, Section::crops,
                WhitelistEntry.LIST_CODEC, Section::players,
                Section::new);

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Expel(@NotNull UUID target) implements PanelAction {

        public static final PanelAction.Type<Expel> TYPE = new PanelAction.Type<>(Expel.class,
                UUIDUtil.STREAM_CODEC.map(Expel::new, Expel::target));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return LandMembership.expel(context.level(), context.data(), target)
                    ? LandNoticeEvent.text("expelled") : LandNoticeEvent.text("expel_failed");
        }
    }

    public record ExpelOutsiders() implements PanelAction {

        public static final PanelAction.Type<ExpelOutsiders> TYPE = new PanelAction.Type<>(ExpelOutsiders.class,
                StreamCodec.unit(new ExpelOutsiders()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return LandNoticeEvent.text("expelled_outsiders",
                    LandMembership.expelOutsiders(context.level(), context.data()));
        }
    }

    public record SetWhitelisted(@NotNull UUID target, boolean listed) implements PanelAction {

        public static final PanelAction.Type<SetWhitelisted> TYPE = new PanelAction.Type<>(SetWhitelisted.class,
                StreamCodec.composite(UUIDUtil.STREAM_CODEC, SetWhitelisted::target, ByteBufCodecs.BOOL,
                        SetWhitelisted::listed, SetWhitelisted::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return LandMembership.setWhitelisted(context.player().server, context.data(), target, listed)
                    ? Component.empty() : LandNoticeEvent.text("player_offline");
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.INFO; }

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
        for (var herd : LandCensus.take(level, HerdWork.CENSUS).values()) animals += herd.size();
        int crops = 0;
        for (FarmWork.Count count : LandCensus.take(level, data, FarmWork.CENSUS).values()) {
            crops += count.mature() + count.growing();
        }
        return new Section(data.territory().acres(), GutsStones.get(level).total(), SoulsunderMountain.GUTS_STONE_CAP,
                animals, crops, present(level, data));
    }

    private static List<WhitelistEntry> present(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        List<WhitelistEntry> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            players.add(new WhitelistEntry(id, player.getGameProfile().getName(), data.isWhitelisted(id),
                    data.isOwner(id)));
        }
        return players;
    }
}
