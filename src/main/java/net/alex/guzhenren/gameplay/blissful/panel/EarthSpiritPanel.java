package net.alex.guzhenren.gameplay.blissful.panel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.blissful.LandCensus;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.LandModules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板的服务端路由：打开、算快照、复判后执行 {@link PanelAction}、刷新。各页的数据与操作在各自模块里。
 * <ul>
 *   <li>{@link #open}：主人打开面板，停在他上次看的页（{@link #remember}，只记在内存里，停服清空；没记过即地图页），先让各模块
 *       {@code opened} 重置按人记的页面状态；无主时别人只收到认主条件，有主时只收到主人名字。</li>
 *   <li>{@link #handle} 先复判：玩家开着的是地灵面板、面板仍有效（地灵活着、距离、仍是主人），再执行操作；操作后清掉
 *       本刻的清点缓存，结果写进下一份快照的提示，立即下发一次。</li>
 *   <li>{@link #snapshot} 只算一页：该页每个模块各一段（{@link LandModules#on}）；由每秒刷新（{@code BlissfulLands}）与
 *       操作后刷新调用。</li>
 * </ul>
 */

public final class EarthSpiritPanel {

    private EarthSpiritPanel() {}

    public static final String MSG_UNOWNED = "guzhenren.blissful_land.spirit.unowned";
    public static final String MSG_OWNED = "guzhenren.blissful_land.spirit.owned";
    private static final String NOTICE = "guzhenren.blissful_land.panel.";
    private static final Map<UUID, Page> LAST_PAGE = new HashMap<>();

    //region Open [打开]
    public static void open(@NotNull ServerPlayer player, @NotNull EarthSpiritEntity spirit) {
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        if (!data.hasOwner()) {
            player.sendSystemMessage(Component.translatable(MSG_UNOWNED, BlissfulLand.SUMMIT_RADIUS));
            return;
        }
        if (!data.isOwner(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(MSG_OWNED, data.ownerName()));
            return;
        }
        for (LandModule<?> module : LandModules.ALL) module.opened(player.getUUID());
        PanelSnapshot snapshot = snapshot((ServerLevel) spirit.level(), data, player.getUUID(), lastPage(player),
                Component.empty());
        player.openMenu(new SimpleMenuProvider((id, inventory, who) -> new EarthSpiritMenu(id, inventory, spirit,
                        snapshot), spirit.getDisplayName()),
                buffer -> PanelSnapshot.STREAM_CODEC.encode(buffer, snapshot));
    }

    public static void remember(@NotNull ServerPlayer player, @NotNull Page page) {
        LAST_PAGE.put(player.getUUID(), page);
    }

    public static @NotNull Page lastPage(@NotNull ServerPlayer player) {
        return LAST_PAGE.getOrDefault(player.getUUID(), Page.MAP);
    }

    public static void clear() { LAST_PAGE.clear(); }

    public static void refresh(@NotNull ServerPlayer player, @NotNull Component notice) {
        if (!(player.containerMenu instanceof EarthSpiritMenu menu) || menu.getSpirit() == null) return;

        EarthSpiritEntity spirit = menu.getSpirit();
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        ModEvents.post(new EarthSpiritPanelEvent(player, snapshot((ServerLevel) spirit.level(), data, player.getUUID(),
                menu.page(), notice)));
    }

    public static @NotNull PanelSnapshot snapshot(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                                  @NotNull UUID viewer, @NotNull Page page,
                                                  @NotNull Component notice) {
        PanelContext context = new PanelContext(level, data, viewer);
        List<PanelSection> sections = new ArrayList<>();
        for (LandModule<?> module : LandModules.on(page)) sections.add(module.section(context));
        return new PanelSnapshot(data.land().nameKey(), notice, page, sections);
    }
    //endregion

    //region Actions [操作]
    public static void handle(@NotNull ServerPlayer player, @NotNull PanelAction action) {
        if (!(player.containerMenu instanceof EarthSpiritMenu menu) || menu.getSpirit() == null
                || !menu.stillValid(player)) {
            return;
        }
        EarthSpiritEntity spirit = menu.getSpirit();
        ServerLevel level = (ServerLevel) spirit.level();
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        Component notice = action.perform(new ActionContext(player, menu, level, data));
        LandCensus.invalidate(level);
        refresh(player, notice);
    }

    public static @NotNull Component notice(@NotNull String key, Object... args) {
        return Component.translatable(NOTICE + key, args);
    }
    //endregion
}
