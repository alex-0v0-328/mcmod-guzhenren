package net.alex.guzhenren.gameplay.aperture.awakening;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 希望蛊[Hope Gu]仪式开出第一空窍的开窍[Awakening]完成事件：由 {@link AwakeningRitualService} 在仪式开窍点同步发布到游
 * 戏总线（管理员命令直接开窍不发布），供上层 {@code advancement} 包触发进度条件而无需反向依赖。
 */

public class ApertureAwakenedEvent extends Event {

    private final ServerPlayer player;

    public ApertureAwakenedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
