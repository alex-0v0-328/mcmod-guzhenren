package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Runs {@link FlightService#tick} at the end of every server player tick.
 *
 * <p>⚠ Loader glue only: the event types differ between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port,
 * so the handler lives here and {@link FlightService} stays identical between the two.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class FlightEvents {

    private FlightEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer player) FlightService.tick(player);
    }
}
