package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.place.PlaceTitle;
import net.alex.guzhenren.gameplay.place.PlaceTitleEvent;
import net.alex.guzhenren.network.payload.PlaceTitlePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 地名标题[Place Title]的服务端转发器：响应 {@link PlaceTitleEvent}，把标题经 {@link PlaceTitlePayload}（由
 * {@link ModPayloads} 注册）下发给该玩家的客户端。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlaceTitleNotices {

    private PlaceTitleNotices() {}

    @SubscribeEvent
    public static void onPlaceTitle(PlaceTitleEvent event) {
        PlaceTitle title = event.getTitle();
        PacketDistributor.sendToPlayer(event.getPlayer(),
                new PlaceTitlePayload(title.kindKey(), title.nameKey(), title.color()));
    }
}
