package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link BodyService} tempers a player's tendons [筋] or bones [骨]: the
 * strength capacity, the max health and the knockback resistance they feed are attribute modifiers that live
 * above the body, in {@code gameplay.attribute}, which the body cannot call into without closing a dependency
 * cycle. Dispatch is synchronous, so a subscriber has run before the write returns. A skin posts
 * {@link SkinWornEvent} instead: its reduction is read per hit and needs no refresh.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public class BodyTemperedEvent extends Event {

    private final ServerPlayer player;

    public BodyTemperedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
