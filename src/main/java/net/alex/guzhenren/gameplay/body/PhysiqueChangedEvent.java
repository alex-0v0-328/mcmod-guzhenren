package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on {@code NeoForge.EVENT_BUS} after {@link BodyService} changes a player's physiques [体质] or
 * undead state -- add, remove, revive, half-zombie, zombie. Dispatch is synchronous, so a subscriber has
 * run before the write returns.
 *
 * <p>The body posts and does not know who listens: the attack modifier lives above it, in
 * {@code gameplay.attribute}, and calling up into it would close a dependency cycle.
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
