package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link BodyService} 改变体质或不死状态（增、删、复活、临时僵尸、僵尸）后发布的事件；同步分发，写入返回时订阅者已执
 * 行。攻击修饰符在上层 {@code gameplay.attribute}，向上调用会成依赖环，故肉身只发布。
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see ExtremePhysiqueChangedEvent
 * @since 1.0.0
 */

public class PhysiqueChangedEvent extends Event {

    private final ServerPlayer player;

    public PhysiqueChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
