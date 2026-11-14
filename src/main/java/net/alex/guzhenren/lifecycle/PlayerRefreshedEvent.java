package net.alex.guzhenren.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link PlayerDataService} 重建玩家派生属性后经 {@code ModEvents} 发出的事件。
 * <ul>
 *   <li>时机：登录、克隆（死亡或末地返回）、完全重置；同步派发，订阅者在生命周期时刻返回前已跑完。</li>
 *   <li>只发不问谁听：外部模组集成（Epic Fight 耐力）在上层 {@code compat}，向上调用会把生命周期绑死在该模组。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerDataService
 * @since 1.0.0
 */

public class PlayerRefreshedEvent extends Event {

    private final ServerPlayer player;

    public PlayerRefreshedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
