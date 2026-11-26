package net.alex.guzhenren.gameplay.blissful.mountain;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.member.MemberService;
import net.alex.guzhenren.gameplay.blissful.member.Person;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
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
 * {@link net.alex.guzhenren.gameplay.blissful.member.OverviewModule.SetWhitelisted}）。
 * <ul>
 *   <li>两个山上操作的规则在 {@link MountainWork}；秘境山心还没解出（Gu World 未装或山未生成）时提示「尚未成形」。</li>
 *   <li>催动成功不报统计（Alex 定），失败才提示。</li>
 * </ul>
 */

public final class MountainModule implements LandModule<MountainModule.Section> {

    public static final MountainModule INSTANCE = new MountainModule();
    public static final int NAME_LENGTH = 16;

    private MountainModule() {}

    public record Section(int gutsStones, int gutsCap, int moveCooldown, @NotNull List<Person> whitelist)
            implements PanelSection {

        public static final Section EMPTY = new Section(0, 0, 0, List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Section::gutsStones,
                ByteBufCodecs.VAR_INT, Section::gutsCap,
                ByteBufCodecs.VAR_INT, Section::moveCooldown,
                Person.LIST_CODEC, Section::whitelist,
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
            long cooldown = MountainWork.moveCooldown(level, data);
            if (cooldown > 0) return EarthSpiritPanel.notice("cooldown", EarthSpiritPanel.seconds(cooldown));

            RealmSite site = SecretRealms.siteOf(level.getServer(), data.land().realm());
            if (site == null) return EarthSpiritPanel.notice("no_mountain");
            return EarthSpiritPanel.notice("moved", MountainWork.moveMobsUp(level, site, data, level.getRandom()));
        }
    }

    public record DriveGutsGu() implements PanelAction {

        public static final PanelAction.Type<DriveGutsGu> TYPE = new PanelAction.Type<>(DriveGutsGu.class,
                StreamCodec.unit(new DriveGutsGu()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            RealmSite site = SecretRealms.siteOf(context.level().getServer(), context.data().land().realm());
            if (site == null) return EarthSpiritPanel.notice("no_mountain");
            return MountainWork.driveGutsGu(context.level(), site, context.player()) == null
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
            return MemberService.addWhitelist(context.player().server, context.data(), name)
                    ? EarthSpiritPanel.notice("whitelist_added", name)
                    : EarthSpiritPanel.notice("player_not_found", name);
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.REALM; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(MoveMobs.TYPE, DriveGutsGu.TYPE, AddWhitelist.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        BlissfulLandData data = context.data();
        List<Person> whitelist = new ArrayList<>();
        data.whitelist().forEach((id, name) -> whitelist.add(new Person(id, name, true, data.isOwner(id))));
        return new Section(GutsStones.get(context.level()).total(), SoulsunderMountain.GUTS_STONE_CAP,
                EarthSpiritPanel.seconds(MountainWork.moveCooldown(context.level(), data)), whitelist);
    }
}
