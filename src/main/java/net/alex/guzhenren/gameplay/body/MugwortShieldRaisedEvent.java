package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link MugwortShieldService#raise} fills the shield, so the path effect it
 * carries can follow from above the body; the shield's end tick is on {@link MugwortShieldService#get}. Dispatch is
 * synchronous, so a subscriber has run before {@code raise} returns.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldDownEvent
 * @since 1.0.0
 */

public class MugwortShieldRaisedEvent extends Event {

    private final ServerPlayer player;

    public MugwortShieldRaisedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
