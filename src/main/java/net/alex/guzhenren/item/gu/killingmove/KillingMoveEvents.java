package net.alex.guzhenren.item.gu.killingmove;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps the store's half of a usable killing move [杀招] fresh on the client: the server re-derives which learned
 * moves have a partner in an aperture store [空窍存储] ({@link KillingMoveService#refreshStoreBacked}) every
 * {@link #REFRESH_TICKS} ticks and whenever a menu closes, the store menu being the player's own way to change it.
 * An unchanged answer is not synced again.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class KillingMoveEvents {

    public static final int REFRESH_TICKS = 10;

    private KillingMoveEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % REFRESH_TICKS == 0) {
            KillingMoveService.refreshStoreBacked(player);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) KillingMoveService.refreshStoreBacked(player);
    }
}
