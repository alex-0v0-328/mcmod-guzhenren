package net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.LandModule;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.ActionContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSection;
import net.alex.guzhenren.gameplay.immortalaperture.member.WhitelistEntry;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandMembership;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/**
 * 「荡魂山」页模块：胆石进度、「挪生物上山」「胆识蛊催动」、荡魂山白名单（按名字添加；移出走总览页的
 * {@link net.alex.guzhenren.gameplay.immortalaperture.member.OverviewModule.SetWhitelisted}）。
 * <ul>
 *   <li>两个山上操作的规则在 {@link SoulsunderMountainWork}；秘境山心还没解出（Gu World 未装或山未生成）时提示「尚未成形」。</li>
 *   <li>催动成功不报统计（Alex 定），失败才提示。</li>
 * </ul>
 */

public final class MountainModule implements LandModule<MountainModule.Section> {

    public static final MountainModule INSTANCE = new MountainModule();
    public static final int NAME_LENGTH = 16;

    private MountainModule() {}

    public record Section(int gutsStones, int gutsCap, int moveCooldown, @NotNull List<WhitelistEntry> whitelist)
            implements PanelSection {

        public static final Section EMPTY = new Section(0, 0, 0, List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Section::gutsStones,
                ByteBufCodecs.VAR_INT, Section::gutsCap,
                ByteBufCodecs.VAR_INT, Section::moveCooldown,
                WhitelistEntry.LIST_CODEC, Section::whitelist,
                Section::new);

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    //region Actions [操作]
    public record MoveMobs() implements PanelAction {

        public static final PanelAction.Type<MoveMobs> TYPE = new PanelAction.Type<>(MoveMobs.class,
                StreamCodec.unit(new MoveMobs()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            ServerLevel level = context.level();
            BlissfulLandData data = context.data();
            long cooldown = SoulsunderMountainWork.moveCooldown(level, data);
            if (cooldown > 0) return EarthSpiritPanel.notice("cooldown", Ticks.ceilSeconds(cooldown));

            RealmSite site = SecretRealms.siteOf(level.getServer(), data.land().realm());
            if (site == null) return EarthSpiritPanel.notice("no_mountain");
            return EarthSpiritPanel.notice("moved",
                    SoulsunderMountainWork.moveMobsUp(level, site, data, level.getRandom()));
        }
    }

    public record DriveGutsGu() implements PanelAction {

        public static final PanelAction.Type<DriveGutsGu> TYPE = new PanelAction.Type<>(DriveGutsGu.class,
                StreamCodec.unit(new DriveGutsGu()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            RealmSite site = SecretRealms.siteOf(context.level().getServer(), context.data().land().realm());
            if (site == null) return EarthSpiritPanel.notice("no_mountain");
            return SoulsunderMountainWork.driveGutsGu(context.level(), site, context.player()) == null
                    ? EarthSpiritPanel.notice("outside_mountain") : Component.empty();
        }
    }

    public record AddWhitelist(@NotNull String name) implements PanelAction {

        public static final PanelAction.Type<AddWhitelist> TYPE = new PanelAction.Type<>(AddWhitelist.class,
                ByteBufCodecs.stringUtf8(NAME_LENGTH).map(AddWhitelist::new, AddWhitelist::name));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            String name = this.name.strip();
            if (name.isEmpty()) return Component.empty();
            return LandMembership.addWhitelist(context.player().server, context.data(), name)
                    ? EarthSpiritPanel.notice("whitelist_added", name)
                    : EarthSpiritPanel.notice("player_not_found", name);
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.SECRET_REALM; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(MoveMobs.TYPE, DriveGutsGu.TYPE, AddWhitelist.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        BlissfulLandData data = context.data();
        List<WhitelistEntry> whitelist = new ArrayList<>();
        data.whitelist().forEach((id, name) -> whitelist.add(new WhitelistEntry(id, name, true, data.isOwner(id))));
        return new Section(GutsStones.get(context.level()).total(), SoulsunderMountain.GUTS_STONE_CAP,
                Ticks.ceilSeconds(SoulsunderMountainWork.moveCooldown(context.level(), data)), whitelist);
    }
}
