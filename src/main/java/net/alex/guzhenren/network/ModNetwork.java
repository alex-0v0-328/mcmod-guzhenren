package net.alex.guzhenren.network;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * The network calls the rest of the mod makes: send a client intent to the server, and open a menu that
 * carries extra data to the client.
 *
 * <p>⚠ A version seam: the only place outside {@code ModPayloads} that names the loader's network API.
 * The Forge 1.20.1 port has a class with the same methods over its {@code SimpleChannel}; callers use
 * these names only, so they stay identical between the two.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModPayloads
 * @since 1.0.0
 */

public final class ModNetwork {

    private ModNetwork() {}

    /** Sends one intent record from {@code network.payload} to the server. */
    public static void sendToServer(@NotNull Object intent) {
        PacketDistributor.sendToServer(ModPayloads.wrap(intent));
    }

    public static void openMenu(@NotNull ServerPlayer player, @NotNull MenuProvider provider,
                                @NotNull Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData::accept);
    }
}
