package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍变更事件：由 {@link ApertureService} 落盘后在游戏总线上同步发布。
 * <ul>
 *   <li>{@link AperturePressureService} 的压力写入绕过此事件；其余写入在返回前已完成所有订阅者回调。</li>
 *   <li>空窍包只发布事件而不直接调用订阅者：最大生命值修饰符位于上层 {@code gameplay.attribute}，直接调用会造成包依
 *       赖环。</li>
 * </ul>
 */

public class ApertureChangedEvent extends Event {

    private final ServerPlayer player;

    public ApertureChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
