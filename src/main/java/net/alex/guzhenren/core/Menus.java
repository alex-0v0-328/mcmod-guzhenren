package net.alex.guzhenren.core;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Opens a menu on a player's client together with the extra data its client side rebuilds from.
 *
 * <p>It lives in {@code core}, not {@code network}, so a domain menu such as the soul trade can open without
 * depending on {@code network}, which dispatches client intents into the domain.
 *
 * <p>⚠ A version seam: the loader's call that opens a menu with extra data lives only here (NeoForge's
 * {@code ServerPlayer.openMenu} with a buffer writer, Forge 1.20.1's {@code NetworkHooks.openScreen}), so the
 * calling lines stay identical between the two. A menu without extra data opens through vanilla's
 * {@code ServerPlayer.openMenu}, the same on both.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Menus {

    private Menus() {}

    /** Opens the menu on the player's client, handing it {@code extraData} to rebuild its client side from. */
    public static void open(@NotNull ServerPlayer player, @NotNull MenuProvider provider,
                            @NotNull Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData::accept);
    }
}
