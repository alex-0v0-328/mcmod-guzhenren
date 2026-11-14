package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link BodyService#shedSkin} 蜕皮（含耗尽玉皮）后发布的事件；同步分发，写入返回时订阅者已执行。不绘制，但永久皮的路径
 * 效果须移除，它在上层 {@code item}，直接调用会成依赖环。
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinWornEvent
 * @since 1.0.0
 */

public class SkinShedEvent extends Event {

    private final ServerPlayer player;

    public SkinShedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
