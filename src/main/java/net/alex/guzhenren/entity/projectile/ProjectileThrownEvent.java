package net.alex.guzhenren.entity.projectile;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus, on the server, each time a player throws one projectile by hand: today every Moon
 * Blade [月刃] ({@link MoonBladeEntity#shoot}), one event a blade. The thrower's body follows from it: the Epic Fight
 * bridge plays the throw clips ({@code EpicFightServerEvents}), so a later throw-type skill gets them by posting this
 * event and wires nothing else. Dispatch is synchronous and the projectile is already in the level.
 *
 * <p>The projectile posts and does not know who listens; a listener changes nothing about the throw.
 *
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public class ProjectileThrownEvent extends Event {

    private final ServerPlayer player;

    public ProjectileThrownEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
