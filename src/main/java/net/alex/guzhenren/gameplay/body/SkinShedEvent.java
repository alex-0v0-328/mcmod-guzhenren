package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link BodyService#shedSkin} takes a player's skin [皮肤] off, a jade skin
 * that ran out included: nothing is drawn for it, but the path effect a permanent skin carried must go, and
 * that lives above the body, in {@code item}, which the body cannot call into without closing a dependency cycle.
 * Dispatch is synchronous, so a subscriber has run before the write returns.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinWornEvent
 * @since 1.0.0
 */

public class SkinShedEvent extends Event {

    private final ServerPlayer player;

    public SkinShedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
