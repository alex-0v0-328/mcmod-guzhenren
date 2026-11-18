package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent.Change;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu]蓄力状态变更的服务端通知转发器：响应 {@link StoreStrengthChargeEvent} 并通过
 * {@link StoreStrengthNoticePayload}（由 {@link ModPayloads} 注册）下发至蓄力客户端；当
 * {@link ClientIntents#storeStrength} 拒绝起手时亦主动补发 {@link Change#DROPPED}。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StoreStrengthNotices {

    private StoreStrengthNotices() {}

    @SubscribeEvent
    public static void onCharge(StoreStrengthChargeEvent event) { send(event.getPlayer(), event.getChange()); }

    static void send(@NotNull ServerPlayer player, @NotNull Change change) {
        PacketDistributor.sendToPlayer(player, new StoreStrengthNoticePayload(change));
    }
}
