package net.alex.guzhenren.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 当 {@link PlayerDataService} 在玩家登录、死亡或末地克隆、或全量重置期间重建完派生属性后，通过 {@code ModEvents} 同步
 * 派发的通知事件。事件返回前所有订阅者均已执行完毕，从而让上层 {@code compat} 模块（例如 {@code EpicFightServerEvents}
 * 据此重读 Epic Fight 耐力上限）解耦监听，避免生命周期层反向依赖外部兼容模组。
 */

public class PlayerRefreshedEvent extends Event {

    private final ServerPlayer player;

    public PlayerRefreshedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
