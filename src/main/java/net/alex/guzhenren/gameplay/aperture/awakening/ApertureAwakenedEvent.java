package net.alex.guzhenren.gameplay.aperture.awakening;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 希望蛊[Hope Gu]仪式开出第一空窍[Aperture]的开窍[Awakening]事件。
 * <ul>
 *   <li>仪式结束时由 {@link AwakeningRitualService} 同步发到游戏总线；命令开窍不发。</li>
 *   <li>仪式不知谁在听：进度条件在上层 {@code advancement} 包，直调即成依赖环。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @since 1.0.0
 */

public class ApertureAwakenedEvent extends Event {

    private final ServerPlayer player;

    public ApertureAwakenedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
