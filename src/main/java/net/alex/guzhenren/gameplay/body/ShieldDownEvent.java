package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身护盾层结束事件：当护盾被伤害击碎、心跳检测到期或由 {@link ShieldService#dismiss} 主动撤除时同步发布，通知上层移
 * 除伴随该护盾挂载的流派效果与动作状态。
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
