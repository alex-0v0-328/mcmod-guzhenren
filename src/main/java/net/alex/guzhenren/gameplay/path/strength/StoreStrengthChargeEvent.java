package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus when the server's own tick changes a Store Strength Gu [蓄力蛊] charge [蓄力] the
 * client cannot see: {@link Change#FULL} the tick it first counts as full ({@link StoreStrength#isFull}), and
 * {@link Change#DROPPED} when the server drops it on its own (a lost buff, branch or fist, or a dimension change).
 * The client's own release and cancel post nothing. Dispatch is synchronous.
 *
 * <p>The charge posts and does not know who listens: the notice to the charging client lives in
 * {@code net.alex.guzhenren.network}, so the client shows the charge full only once the server holds it full.
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrength
 * @since 1.0.0
 */

public class StoreStrengthChargeEvent extends Event {

    public enum Change { FULL, DROPPED }

    private final ServerPlayer player;
    private final Change change;

    public StoreStrengthChargeEvent(@NotNull ServerPlayer player, @NotNull Change change) {
        this.player = player;
        this.change = change;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Change getChange() { return change; }
}
