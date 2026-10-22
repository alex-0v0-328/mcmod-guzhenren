package net.alex.guzhenren.network;

import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.packet.PlayerDataSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

/**
 * The mod's one network channel, {@code guzhenren:main}, and the packets registered on it.
 *
 * <p>Both sides must run the same {@link #VERSION}; a client with another version is refused at login.
 * Message ids follow registration order in {@link #register}.
 *
 * <p>⚠ Append new packets at the end of {@link #register}: inserting one shifts every later id and breaks
 * a client and server built from different revisions in a way the version check cannot see.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModNetwork {

    private ModNetwork() {}

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Guzhenren.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(PlayerDataSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerDataSyncPacket::encode)
                .decoder(PlayerDataSyncPacket::decode)
                .consumerMainThread(PlayerDataSyncPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    /** Opens a menu on the player's client, handing it {@code extraData} to rebuild its client side from. */
    public static void openMenu(@NotNull ServerPlayer player, @NotNull MenuProvider provider,
                                @NotNull Consumer<FriendlyByteBuf> extraData) {
        NetworkHooks.openScreen(player, provider, extraData);
    }
}
