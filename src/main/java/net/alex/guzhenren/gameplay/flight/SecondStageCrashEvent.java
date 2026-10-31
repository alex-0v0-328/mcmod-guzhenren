package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus when a second-stage flight [二段飞行] crashes into a wall, a floor or a ceiling, on
 * whichever end moved the player into it ({@link SecondStageService#travel}): the server's step, with a
 * {@code ServerPlayer}, and the local player's own physics, with a {@code LocalPlayer}. Listeners check which they
 * hold. Dispatch is synchronous, so the player still stands against the surface; {@link #getHeading} is the unit
 * direction it was flying in, pitch included, so a dive's heading points down.
 *
 * <p>Flight posts and does not know who listens: the crash ring lives in {@code net.alex.guzhenren.particle}, the
 * shake in {@code net.alex.guzhenren.client.event.SecondStageCamera}.
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
 */

public class SecondStageCrashEvent extends Event {

    private final Player player;
    private final Vec3 heading;

    public SecondStageCrashEvent(@NotNull Player player, @NotNull Vec3 heading) {
        this.player = player;
        this.heading = heading;
    }

    public @NotNull Player getPlayer() { return player; }

    public @NotNull Vec3 getHeading() { return heading; }
}
