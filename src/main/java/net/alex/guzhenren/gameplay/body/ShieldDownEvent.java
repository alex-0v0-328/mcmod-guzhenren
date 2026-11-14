package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 护盾层结束后发布的事件：打空它的伤害、发现超时的心跳或 {@link ShieldService#dismiss}；其携带的路径效果须随之移除。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldRaisedEvent
 * @since 1.0.0
 */

public class ShieldDownEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;

    public ShieldDownEvent(@NotNull ServerPlayer player, @NotNull ShieldType type) {
        this.player = player;
        this.type = type;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }
}
