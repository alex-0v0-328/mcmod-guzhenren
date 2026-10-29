package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on the game event bus after {@link BodyService#wearSkin} puts a skin [皮肤] on a player, jade or permanent; the
 * flash every watching client draws is sent by a subscriber in {@code network}, which the body cannot call
 * into without closing a dependency cycle. Shedding a skin posts {@link SkinShedEvent} instead.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public class SkinWornEvent extends Event {

    private final ServerPlayer player;
    private final Skin skin;

    public SkinWornEvent(@NotNull ServerPlayer player, @NotNull Skin skin) {
        this.player = player;
        this.skin = skin;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Skin getSkin() { return skin; }
}
