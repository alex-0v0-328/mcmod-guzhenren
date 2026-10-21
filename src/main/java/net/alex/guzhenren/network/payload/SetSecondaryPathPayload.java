package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client intent: choose the secondary path [辅修] on the named aperture [空窍].
 *
 * <p>A payload carrying only an aperture index and a nullable {@link GuPath}
 * -- no other player data. The server handler in {@link net.alex.guzhenren.network.ModPayloads}
 * writes the choice through the aperture service. The path travels as its ordinal plus one (zero =
 * unset) via {@link ModByteBufs#writeNullableEnum}.
 *
 * <p>⚠ It is a payload only because the screen that sends it has no menu behind it. Anything inside a
 * container sends its intent over vanilla's own channels instead.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record SetSecondaryPathPayload(int aperture, @Nullable GuPath path) implements CustomPacketPayload {

    public static final Type<SetSecondaryPathPayload> TYPE = new Type<>(
            Guzhenren.id("set_secondary_path"));
    public static final StreamCodec<FriendlyByteBuf, SetSecondaryPathPayload> STREAM_CODEC =
            StreamCodec.of(SetSecondaryPathPayload::encode, SetSecondaryPathPayload::decode);

    public static @NotNull SetSecondaryPathPayload decode(@NotNull FriendlyByteBuf buf) {
        return new SetSecondaryPathPayload(buf.readVarInt(), ModByteBufs.readNullableEnum(buf, GuPath.class));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull SetSecondaryPathPayload value) {
        buf.writeVarInt(value.aperture());
        ModByteBufs.writeNullableEnum(buf, value.path());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
