package net.alex.guzhenren.gameplay.aperture.awakening;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus when {@link AwakeningRitualService} awakens a player's first aperture [空窍] at the
 * end of the Hope Gu ritual [希望蛊]; an awakening granted by command posts nothing. Dispatch is synchronous.
 *
 * <p>The ritual posts and does not know who listens: the Hope Gu criterion lives above it, in
 * {@code net.alex.guzhenren.advancement}, and calling up into it would close a dependency cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @since 1.0.0
 */

public class ApertureAwakenedEvent extends Event {

    private final ServerPlayer player;

    public ApertureAwakenedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
