package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍[Aperture]变更事件，{@link ApertureService} 存储后同步发布。
 *
 * <ul>
 *   <li>{@link AperturePressureService} 的压力写入不发布；写入返回前订阅者已执行完。</li>
 *   <li>空窍只发布、不认识订阅者：最大生命值修饰符在上层 {@code gameplay.attribute}，向上调用会成依赖环。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @since 1.0.0
 */

public class ApertureChangedEvent extends Event {

    private final ServerPlayer player;

    public ApertureChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
