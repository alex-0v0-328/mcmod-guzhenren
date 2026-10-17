package net.alex.guzhenren.network.payload;

import io.netty.buffer.ByteBuf;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Client intent: open the refinement [炼蛊] menu.
 *
 * <p>A zero-byte singleton payload -- it carries no data at all, only the button press. The server
 * handler in {@link net.alex.guzhenren.network.ModPayloads} opens the
 * {@link RefinementMenu}. Client intent is the one direction attachment
 * sync cannot carry; no player data travels upstream.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record OpenRefinementPayload() implements CustomPacketPayload {

    public static final OpenRefinementPayload INSTANCE = new OpenRefinementPayload();
    public static final Type<OpenRefinementPayload> TYPE = new Type<>(
            Guzhenren.id("open_refinement"));
    public static final StreamCodec<ByteBuf, OpenRefinementPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
