package net.alex.guzhenren.particle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageStartedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives {@link SecondStageRingEmitter}: the sonic boom on each {@link SecondStageStartedEvent}, the sound barrier
 * rings at the end of every player tick, and dropping their trail on logout and on a dimension change.
 *
 * <p>⚠ Loader glue only: the event types differ between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port,
 * so the handlers live here and {@link SecondStageRingEmitter} stays identical between the two.
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageRingEmitter
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class SecondStageRingEvents {

    private SecondStageRingEvents() {}

    @SubscribeEvent
    public static void onSecondStageStarted(SecondStageStartedEvent event) {
        SecondStageRingEmitter.sonicBoom(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) SecondStageRingEmitter.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SecondStageRingEmitter.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SecondStageRingEmitter.forget(event.getEntity());
    }
}
