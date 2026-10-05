package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on {@code NeoForge.EVENT_BUS} after {@link BodyService#setExtremePhysique} stored a different
 * ten-extreme physique [十绝体]; {@link #getBefore} and {@link #getAfter} always differ. Dispatch is
 * synchronous, so a subscriber has run before the write returns.
 *
 * <p>The aperture reacts to it (talent paths, base essence, pressure); the body posts instead of calling
 * the aperture, which already depends on the body.
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
