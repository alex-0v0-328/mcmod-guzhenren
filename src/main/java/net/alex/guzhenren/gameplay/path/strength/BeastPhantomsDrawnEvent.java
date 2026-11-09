package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus when {@link StrengthRound} sets a player's beast phantoms to the families of an
 * attack round [攻击轮次]: at its opening, and again the tick a drawn family leaves it. Dispatch is synchronous, so the
 * phantoms stand before the post returns.
 *
 * <p>The round posts and does not know who listens: the phantom entity lives above it, in
 * {@code net.alex.guzhenren.entity.beast}, and calling up into it would close a dependency cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthRound
 * @since 1.0.0
 */

public class BeastPhantomsDrawnEvent extends Event {

    private final ServerPlayer player;
    private final List<BeastStrengthFamily> families;

    public BeastPhantomsDrawnEvent(@NotNull ServerPlayer player, @NotNull List<BeastStrengthFamily> families) {
        this.player = player;
        this.families = List.copyOf(families);
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull List<BeastStrengthFamily> getFamilies() { return families; }
}
