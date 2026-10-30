package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands the awakening ritual [开窍仪式] its events: {@link AwakeningRitualService#tick} at the end of every server
 * player tick, and {@link AwakeningRitualService#finish} on a death, a dimension change, a logout and a login.
 *
 * <p>The event handlers live here, the rules behind them in {@link AwakeningRitualService}. ⚠ The death hook runs
 * before the clone, so a death without keepInventory drops the aperture the ritual just opened, as any death of an
 * awakened player does.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AwakeningRitualEvents {

    private AwakeningRitualEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.tick(player);
    }

    // Last, so a handler that cancels the death (a revive) leaves the ritual running.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }
}
