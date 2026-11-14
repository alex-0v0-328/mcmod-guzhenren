package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link BodyService#setExtremePhysique} 存入不同十绝体后发布的事件；同步分发，写入返回时订阅者已执行，
 * {@link #getBefore} 与 {@link #getAfter} 必不同。空窍[Aperture]响应（天赋道、基础真元[Essence]、压力）；空窍已依赖肉
 * 身，故不直接调用。
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public class ExtremePhysiqueChangedEvent extends Event {

    private final ServerPlayer player;
    private final ExtremePhysique before;
    private final ExtremePhysique after;

    public ExtremePhysiqueChangedEvent(@NotNull ServerPlayer player, @NotNull ExtremePhysique before,
                                       @NotNull ExtremePhysique after) {
        this.player = player;
        this.before = before;
        this.after = after;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ExtremePhysique getBefore() { return before; }

    public @NotNull ExtremePhysique getAfter() { return after; }
}
