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
 * 蓄力蛊[Store Strength Gu]蓄力通知转发：服务端 {@link StoreStrengthChargeEvent} 以 {@link StoreStrengthNoticePayload}
 * 发给蓄力客户端；{@link ClientIntents#storeStrength} 拒起手时也发 {@link Change#DROPPED}。
 *
 * @author Alex
 * @version 1.0.0
 * @see ModPayloads
 * @since 1.0.0
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
