package net.alex.guzhenren.entity.projectile;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家亲手投出弹射物时在 NeoForge 游戏事件总线同步发布的服务端事件。
 * <ul>
 *   <li>目前每发射一枚月刃（{@link MoonBladeEntity#shoot}）即发布一次；Epic Fight 桥接层
 *       （{@code EpicFightServerEvents}）监听本事件以播放投掷动作，新增投掷类技能只需发布本事件即可复用表现，发布方无需
 *       感知监听者。</li>
 * </ul>
 */

public class ProjectileThrownEvent extends Event {

    private final ServerPlayer player;

    public ProjectileThrownEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
