package net.alex.guzhenren.gameplay.flight;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]启动事件：{@link SecondStageService#start} 每次进入二段飞行时在起点同步发布，供上层
 * {@code net.alex.guzhenren.particle} 播放音爆环与音效，避免 {@code gameplay.flight} 反向依赖。
 */

public class SecondStageStartedEvent extends Event {

    private final ServerPlayer player;

    public SecondStageStartedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
