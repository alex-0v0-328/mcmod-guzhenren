package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 第一空窍插入事件：由 {@link ApertureService} 在孤立第二空窍前插入第一空窍（{@link ApertureData#insertFirst}）后同步发
 * 布。
 * <ul>
 *   <li>按空窍下标保存的数据列表（如空窍存储）在回调中整体后移一位；开窍[Awakening]调用返回前已完成移位。</li>
 *   <li>{@code gameplay.aperture} 只发布事件而不认识订阅者：空窍存储位于子包 {@code gameplay.aperture.storage}，直接调
 *       用会造成双向依赖。</li>
 * </ul>
 */

public class FirstApertureInsertedEvent extends Event {

    private final ServerPlayer player;

    public FirstApertureInsertedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
