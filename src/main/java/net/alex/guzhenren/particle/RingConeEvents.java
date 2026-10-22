package net.alex.guzhenren.particle;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives {@link RingConeEmitter}: advances a player's ring trails at the end of every player tick, and drops them
 * on logout and on a dimension change.
 *
 * <p>⚠ Loader glue only: the event types differ between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port,
 * so the handlers live here and {@link RingConeEmitter} stays identical between the two.
 *
 * @author Alex
 * @version 1.0.0
 * @see RingConeEmitter
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class RingConeEvents {

    private RingConeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) RingConeEmitter.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RingConeEmitter.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        RingConeEmitter.forget(event.getEntity());
    }
}
