package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Client intent: open the store of the named aperture [空窍].
 *
 * <p>A zero-data-except-an-index payload -- client intent is the one direction attachment sync cannot
 * carry, and this carries only which aperture to open. The server handler in
 * {@link net.alex.guzhenren.network.ModPayloads} opens the
 * {@link ApertureStorageMenu}; no player data travels upstream.
 *
 * <p>⚠ No payload may carry player data -- that always travels the other way, as synced state.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record OpenApertureStoragePayload(int aperture) {

    public static @NotNull OpenApertureStoragePayload decode(@NotNull FriendlyByteBuf buf) {
        return new OpenApertureStoragePayload(buf.readVarInt());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull OpenApertureStoragePayload value) {
        buf.writeVarInt(value.aperture());
    }
}
