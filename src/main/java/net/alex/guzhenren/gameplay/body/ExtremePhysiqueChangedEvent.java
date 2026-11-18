package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 十绝体[Ten Extreme Physiques]变更事件：{@link BodyService#setExtremePhysique} 写入不同的十绝体后同步发布
 * （{@link #getBefore} 与 {@link #getAfter} 必定不同），供上层 {@code gameplay.aperture} 对齐天赋流派效果、第一空窍基础
 * 真元与压力。
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
