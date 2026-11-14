package net.alex.guzhenren.gameplay.flight;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link SecondStageService#start} 每次进入二段飞行[Second-Stage Flight]时在游戏事件总线发布。同步派发，玩家仍在起点；
 * 飞行包只发布，音爆在上层 {@code net.alex.guzhenren.particle}，向上调用会成依赖环。
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
 */

public class SecondStageStartedEvent extends Event {

    private final ServerPlayer player;

    public SecondStageStartedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
