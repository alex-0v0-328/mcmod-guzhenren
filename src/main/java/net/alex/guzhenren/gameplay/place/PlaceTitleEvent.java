package net.alex.guzhenren.gameplay.place;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 地名标题显示事件：{@link PlaceTitles} 决定给某玩家弹出标题时同步发布，由网络层转成下发给该玩家客户端的通知。
 */

public class PlaceTitleEvent extends Event {

    private final ServerPlayer player;
    private final PlaceTitle title;

    public PlaceTitleEvent(@NotNull ServerPlayer player, @NotNull PlaceTitle title) {
        this.player = player;
        this.title = title;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull PlaceTitle getTitle() { return title; }
}
