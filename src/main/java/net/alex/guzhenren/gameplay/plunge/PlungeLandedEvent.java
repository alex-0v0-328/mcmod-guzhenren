package net.alex.guzhenren.gameplay.plunge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus when a plunge [坠击] lands hard enough to show ({@link PlungeImpact#getImpactPower}
 * above zero), before the crater is blasted, so the ground at {@link #getPosition} is still whole. Dispatch is
 * synchronous.
 *
 * <p>The plunge posts and does not know who listens: the ground fracture lives in {@code net.alex.guzhenren.compat},
 * the shake in {@code net.alex.guzhenren.network}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

public class PlungeLandedEvent extends Event {

    private final ServerPlayer player;
    private final Vec3 position;
    private final float impactPower;

    public PlungeLandedEvent(@NotNull ServerPlayer player, @NotNull Vec3 position, float impactPower) {
        this.player = player;
        this.position = position;
        this.impactPower = impactPower;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Vec3 getPosition() { return position; }

    public float getImpactPower() { return impactPower; }
}
