package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link ApertureNourishService} raised a stage or rank and wrote the
 * aperture [空窍], carrying the aperture as it stands. Dispatch is synchronous.
 *
 * <p>The nourishing posts and does not know who listens: the realm criterion lives above it, in
 * {@code net.alex.guzhenren.advancement}, and calling up into it would close a dependency cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureNourishService
 * @since 1.0.0
 */

public class RealmReachedEvent extends Event {

    private final ServerPlayer player;
    private final Aperture aperture;

    public RealmReachedEvent(@NotNull ServerPlayer player, @NotNull Aperture aperture) {
        this.player = player;
        this.aperture = aperture;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Aperture getAperture() { return aperture; }
}
