package net.alex.guzhenren.gameplay.plunge;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands the plunge [坠击] its events: {@link PlungeService#tick} at the end of every server player tick and
 * {@link PlungeService#tickLocal} at the end of the local player's; vanilla's fall damage canceled while a plunge
 * runs, since the landing deals its own; a dimension change or a teleport ends the plunge.
 *
 * <p>The event handlers live here, the rules behind them in {@link PlungeService}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlungeEvents {

    private PlungeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            PlungeService.tick(serverPlayer);
        } else if (player.isLocalPlayer()) {
            PlungeService.tickLocal(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && PlungeService.isActive(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlungeService.stop(player);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlungeService.stop(player);
    }
}
