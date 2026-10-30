package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link ShieldService#raise} lays a shield [护盾] layer, so the path effect its
 * Gu carries can follow from above the body; the layer's end tick is on {@link ShieldService#find}. Dispatch is
 * synchronous, so a subscriber has run before {@code raise} returns.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldDownEvent
 * @since 1.0.0
 */

public class ShieldRaisedEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;

    public ShieldRaisedEvent(@NotNull ServerPlayer player, @NotNull ShieldType type) {
        this.player = player;
        this.type = type;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }
}
