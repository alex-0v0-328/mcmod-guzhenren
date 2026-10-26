package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Forwards {@link SkinWornEvent} to the wire: the body posts, this class sends the
 * {@link SkinFlashPayload} to the player and everyone tracking them.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModNetwork
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class SkinFlashEvents {

    private SkinFlashEvents() {}

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) {
        ModNetwork.sendToTrackingAndSelf(event.getPlayer(),
                new SkinFlashPayload(event.getPlayer().getId(), event.getSkin()));
    }
}
