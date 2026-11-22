package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanelEvent;
import net.alex.guzhenren.network.payload.EarthSpiritPanelPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 地灵经营面板的服务端转发器：响应 {@link EarthSpiritPanelEvent}，把快照经 {@link EarthSpiritPanelPayload}（由
 * {@link ModPayloads} 注册）下发给该玩家的客户端。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EarthSpiritNotices {

    private EarthSpiritNotices() {}

    @SubscribeEvent
    public static void onPanel(EarthSpiritPanelEvent event) {
        PacketDistributor.sendToPlayer(event.getPlayer(), new EarthSpiritPanelPayload(event.getSnapshot()));
    }
}
