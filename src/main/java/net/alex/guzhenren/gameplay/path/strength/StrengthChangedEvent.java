package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力[Beast Strength]或人力写入后的同步事件。{@link PathStrengthService} 发到游戏事件总线，力道不知谁听：攻击修饰在上
 * 层 {@code gameplay.attribute}，向上调用会成环。
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public class StrengthChangedEvent extends Event {

    private final ServerPlayer player;

    public StrengthChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
