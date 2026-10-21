package net.alex.guzhenren.client.network;

import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.network.packet.PlayerDataSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The client half of the server-to-client packets, kept apart so a dedicated server never loads it.
 *
 * <p>Runs on the client main thread. A packet that arrives before the local player exists (early in
 * login) is dropped; the full sync after login, respawn and dimension change covers it.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void applyPlayerData(PlayerDataSyncPacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        packet.field().set(PlayerData.get(player), packet.value());
    }
}
