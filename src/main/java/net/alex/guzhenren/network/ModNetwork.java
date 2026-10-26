package net.alex.guzhenren.network;

import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * The network call the rest of the mod makes: send a client intent to the server. A menu that carries extra data
 * to the client opens through {@link net.alex.guzhenren.core.Menus}.
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
}
