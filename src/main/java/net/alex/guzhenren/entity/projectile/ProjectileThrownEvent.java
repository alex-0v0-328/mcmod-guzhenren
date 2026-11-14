package net.alex.guzhenren.entity.projectile;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家亲手投出弹射物的服务端事件，发布在游戏事件总线。
 * <ul>
 *   <li>目前每发月刃（{@link MoonBladeEntity#shoot}）一个；Epic Fight 桥接据此播投掷动画
 *       （{@code EpicFightServerEvents}），新投掷技能只需发布本事件。</li>
 *   <li>同步分发，弹射物已入世界；发布方不知谁在听，监听者不改变投掷。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public class ProjectileThrownEvent extends Event {

    private final ServerPlayer player;

    public ProjectileThrownEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
