package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 第一空窍[Aperture]插入事件，{@link ApertureService} 在孤立第二空窍前存下第一空窍后同步发布。
 *
 * <ul>
 *   <li>按空窍下标保存的列表都后移一位；开窍[Awakening]返回前存储已移位。</li>
 *   <li>空窍只发布、不认识订阅者：各空窍存储在上层 {@code gameplay.aperture.storage}，向上调用会成依赖环。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see ApertureData#insertFirst
 * @since 1.0.0
 */

public class FirstApertureInsertedEvent extends Event {

    private final ServerPlayer player;

    public FirstApertureInsertedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
