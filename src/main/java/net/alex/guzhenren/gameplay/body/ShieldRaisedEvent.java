package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ShieldService#raise} 升起一层护盾（含补满仍在的层）后发布的事件；同步分发，{@code raise} 返回时订阅者已执行。
 * 供上层跟进蛊的路径效果、Epic Fight 桥接交叉双臂（{@code EpicFightServerEvents}）；结束 tick 见
 * {@link ShieldService#find}。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldDownEvent
 * @since 1.0.0
 */

public class ShieldRaisedEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;

    public ShieldRaisedEvent(@NotNull ServerPlayer player, @NotNull ShieldType type) {
        this.player = player;
        this.type = type;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }
}
