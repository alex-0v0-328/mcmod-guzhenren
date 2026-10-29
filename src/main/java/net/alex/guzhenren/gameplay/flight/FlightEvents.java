package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runs {@link FlightService#tick}, then {@link SecondStageService#tick}, at the end of every server player tick: the
 * Gu flight settles first, so the stage sees this tick's flight.
 *
 * <p>The event handlers live here, the rules behind them in the two services; a logout drops the player's ram
 * cooldowns ({@link SecondStageService#forget}).
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class FlightEvents {

    private FlightEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FlightService.tick(player);
            SecondStageService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SecondStageService.forget(event.getEntity());
    }
}
