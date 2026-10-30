package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after a shield [护盾] layer ends: the hit that empties it, the heartbeat that finds
 * its time run out, or {@link ShieldService#dismiss}. The path effect it carried must go with it.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldRaisedEvent
 * @since 1.0.0
 */

public class ShieldDownEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;

    public ShieldDownEvent(@NotNull ServerPlayer player, @NotNull ShieldType type) {
        this.player = player;
        this.type = type;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }
}
