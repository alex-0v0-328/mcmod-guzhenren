package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on {@code NeoForge.EVENT_BUS} after {@link PathStrengthService} wrote a player's beast or human
 * strengths [力道]. Dispatch is synchronous, so a subscriber has run before the write returns.
 *
 * <p>The path posts and does not know who listens: the attack modifier lives above it, in
 * {@code gameplay.attribute}, and calling up into it would close a dependency cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public class StrengthChangedEvent extends Event {

    private final ServerPlayer player;

    public StrengthChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
