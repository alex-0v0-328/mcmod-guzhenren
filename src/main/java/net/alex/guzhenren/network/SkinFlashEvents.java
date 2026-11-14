package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@link SkinWornEvent} 转发：身体侧发事件，本类发 {@link SkinFlashPayload} 给该玩家及所有追踪者。
 *
 * @author Alex
 * @version 1.0.0
 * @see ModPayloads
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class SkinFlashEvents {

    private SkinFlashEvents() {}

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(event.getPlayer(),
                new SkinFlashPayload(event.getPlayer().getId(), event.getSkin()));
    }
}
