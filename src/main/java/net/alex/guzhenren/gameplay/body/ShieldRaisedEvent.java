package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身护盾升起事件：{@link ShieldService#raise} 升起新护盾层（含刷新同种已有层）后同步发布，供上层挂载对应蛊虫的流派效
 * 果与 Epic Fight 交叉双臂防御姿态（{@code EpicFightServerEvents}）；到期时刻可通过 {@link ShieldService#find} 查询。
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
