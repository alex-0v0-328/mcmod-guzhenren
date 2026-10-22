package net.alex.guzhenren.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted through {@code ModEvents} after {@link PlayerDataService} rebuilt a player's derived attributes: on
 * login, on a clone (death or End return), and on a full reset. Dispatch is synchronous, so a subscriber has
 * run before the lifecycle moment returns.
 *
 * <p>The lifecycle posts and does not know who listens: an integration with another mod (Epic Fight's stamina)
 * lives above it, in {@code compat}, and calling up into it would tie the lifecycle to that mod.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerDataService
 * @since 1.0.0
 */

public class PlayerRefreshedEvent extends Event {

    private final ServerPlayer player;

    public PlayerRefreshedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
