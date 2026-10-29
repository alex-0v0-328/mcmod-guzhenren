package net.alex.guzhenren.gameplay.path;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link PathEffectService} drops a path effect [流派效果] from a player, with its
 * marks, naming why: its time ran out, its marks were cut to nothing, another took its slot, or a caller or a
 * command removed it. A family whose effect leaves something behind when it ends subscribes here.
 *
 * <p>⚠ The death that wipes a player resets the whole store past the service and posts nothing; a subscriber resets
 * its own state with that death, as every other player value does.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @since 1.0.0
 */

public class PathEffectEndedEvent extends Event {

    private final ServerPlayer player;
    private final PathEffect<?> effect;
    private final Reason reason;

    public PathEffectEndedEvent(@NotNull ServerPlayer player, @NotNull PathEffect<?> effect, @NotNull Reason reason) {
        this.player = player;
        this.effect = effect;
        this.reason = reason;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull PathEffect<?> getEffect() { return effect; }

    public @NotNull Reason getReason() { return reason; }

    public enum Reason { EXPIRED, CUT, REPLACED, REMOVED }
}
