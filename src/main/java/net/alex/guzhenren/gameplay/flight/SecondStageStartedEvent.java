package net.alex.guzhenren.gameplay.flight;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus each time {@link SecondStageService#start} enters second-stage flight [二段飞行].
 * Dispatch is synchronous, so the player still stands where the stage began.
 *
 * <p>Flight posts and does not know who listens: the sonic boom lives above it, in
 * {@code net.alex.guzhenren.particle}, and calling up into it would close a dependency cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
 */

public class SecondStageStartedEvent extends Event {

    private final ServerPlayer player;

    public SecondStageStartedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
