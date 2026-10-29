package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after the Heavenly Mugwort shield ends: the hit that empties it or the heartbeat that
 * finds its time run out ({@link MugwortShieldService}). The path effect it carried must go with it.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldRaisedEvent
 * @since 1.0.0
 */

public class MugwortShieldDownEvent extends Event {

    private final ServerPlayer player;

    public MugwortShieldDownEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
