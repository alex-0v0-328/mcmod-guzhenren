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
 * <p>The event handlers live here, the trails behind them in {@link RingConeEmitter}.
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
