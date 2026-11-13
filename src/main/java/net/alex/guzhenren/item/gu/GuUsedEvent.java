package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after a mortal Gu's [凡蛊] use has landed and been billed: {@link MortalGuItem}'s
 * {@code spend}, the one step every landed use ends in -- the click, the tap use, an in-place use (a radial menu, a
 * killing move's cast, an Amplify Effect Gu's drive, a Primeval Elder Gu's deposit and withdrawal) and an auto use.
 * A refused use, a use stopped short of hunger, the refining of a wild Gu and a channel's rounds never reach it, and
 * Gu material is no mortal Gu. The Epic Fight bridge plays the generic use clip off it ({@code EpicFightServerEvents}).
 * The stack is not carried: the use may have taken the last Gu of it. Dispatch is synchronous.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public class GuUsedEvent extends Event {

    private final ServerPlayer player;
    private final MortalGuItem gu;

    public GuUsedEvent(@NotNull ServerPlayer player, @NotNull MortalGuItem gu) {
        this.player = player;
        this.gu = gu;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull MortalGuItem getGu() { return gu; }
}
