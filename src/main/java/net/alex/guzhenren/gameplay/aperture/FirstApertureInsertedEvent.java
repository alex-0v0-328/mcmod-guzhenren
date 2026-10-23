package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus right after {@link ApertureService} stored a first aperture [空窍] inserted ahead
 * of a lone second one, so every list kept per aperture index slides up one position. Dispatch is synchronous:
 * the store has shifted before the opening returns.
 *
 * <p>The aperture posts and does not know who listens: the store of each aperture lives above it, in
 * {@code gameplay.aperture.storage}, and calling up into it would close a dependency cycle.
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
