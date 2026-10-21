package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

public record OpenApertureStoragePayload(int aperture) implements CustomPacketPayload {

    public static final Type<OpenApertureStoragePayload> TYPE = new Type<>(
            Guzhenren.id("open_aperture_storage"));
    public static final StreamCodec<FriendlyByteBuf, OpenApertureStoragePayload> STREAM_CODEC =
            StreamCodec.of(OpenApertureStoragePayload::encode, OpenApertureStoragePayload::decode);

    public static @NotNull OpenApertureStoragePayload decode(@NotNull FriendlyByteBuf buf) {
        return new OpenApertureStoragePayload(buf.readVarInt());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull OpenApertureStoragePayload value) {
        buf.writeVarInt(value.aperture());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
