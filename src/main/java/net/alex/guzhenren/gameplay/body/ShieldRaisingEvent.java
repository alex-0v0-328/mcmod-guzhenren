package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus as {@link ShieldService#raise} is about to lay a shield [护盾] layer, so a trait from
 * above the body can change its points before the layer is built: the stone turtle's [石龟负力] half again
 * ({@code BeastTraitEvents}). The layer is laid with whatever {@link #getPoints} holds once dispatch returns, never
 * below zero; its length stays the caller's.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldRaisedEvent
 * @since 1.0.0
 */

public class ShieldRaisingEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;
    private float points;

    public ShieldRaisingEvent(@NotNull ServerPlayer player, @NotNull ShieldType type, float points) {
        this.player = player;
        this.type = type;
        this.points = points;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }

    public float getPoints() { return points; }

    public void setPoints(float points) { this.points = points; }
}
