package net.alex.guzhenren.gameplay.blissful;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板刷新事件：{@link EarthSpiritPanel} 给开着面板的主人算好新快照时同步发布，由网络层转成下发通知。
 */

public class EarthSpiritPanelEvent extends Event {

    private final ServerPlayer player;
    private final PanelSnapshot snapshot;

    public EarthSpiritPanelEvent(@NotNull ServerPlayer player, @NotNull PanelSnapshot snapshot) {
        this.player = player;
        this.snapshot = snapshot;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull PanelSnapshot getSnapshot() { return snapshot; }
}
