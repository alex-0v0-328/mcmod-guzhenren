package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMarksEvent;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTilesEvent;
import net.alex.guzhenren.network.payload.EarthSpiritMapMarksPayload;
import net.alex.guzhenren.network.payload.EarthSpiritMapTilesPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 福地地图的服务端转发器：响应 {@link MapTilesEvent} 与 {@link MapMarksEvent}，经 {@link EarthSpiritMapTilesPayload}、
 * {@link EarthSpiritMapMarksPayload}（由 {@link ModPayloads} 注册）下发给该玩家的客户端。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EarthSpiritMapNotices {

    private EarthSpiritMapNotices() {}

    @SubscribeEvent
    public static void onTiles(MapTilesEvent event) {
        PacketDistributor.sendToPlayer(event.getPlayer(), new EarthSpiritMapTilesPayload(event.getTiles()));
    }

    @SubscribeEvent
    public static void onMarks(MapMarksEvent event) {
        PacketDistributor.sendToPlayer(event.getPlayer(), new EarthSpiritMapMarksPayload(event.getMarks()));
    }
}
