package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on {@code NeoForge.EVENT_BUS} after {@link ApertureService} stored a player's apertures [空窍];
 * pressure writes in {@link AperturePressureService} do not post. Dispatch is synchronous, so a
 * subscriber has run before the write returns.
 *
 * <p>The aperture posts and does not know who listens: the max-health modifier lives above it, in
 * {@code gameplay.attribute}, and calling up into it would close a dependency cycle.
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
