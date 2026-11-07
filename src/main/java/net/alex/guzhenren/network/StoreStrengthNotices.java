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
 * Forwards the server's own Store Strength Gu [蓄力蛊] charge changes ({@link StoreStrengthChargeEvent}) to the
 * charging client as a {@link StoreStrengthNoticePayload}; {@link ClientIntents#storeStrength} sends the same
 * {@link Change#DROPPED} when it refuses a start.
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
