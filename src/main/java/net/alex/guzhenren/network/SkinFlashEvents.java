package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 将肉身侧的 {@link SkinWornEvent} 转发为下行 {@link SkinFlashPayload} 通知（由 {@link ModPayloads} 注册），发送给穿戴
 * 者本人及所有正在追踪该玩家的客户端以播放皮肤闪光效果。
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
