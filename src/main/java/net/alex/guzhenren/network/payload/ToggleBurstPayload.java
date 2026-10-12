package net.alex.guzhenren.network.payload;

import io.netty.buffer.ByteBuf;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for a sprint double tap: start a burst [疾飞], or end the one in progress.
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.flight.FlightService#toggleBurst} decides from the server's own state, and a
 * player without the burst grant simply gets no burst.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.FlightService
 * @since 1.0.0
 */

public record ToggleBurstPayload() implements CustomPacketPayload {

    public static final ToggleBurstPayload INSTANCE = new ToggleBurstPayload();
    public static final Type<ToggleBurstPayload> TYPE = new Type<>(Guzhenren.id("toggle_burst"));
    public static final StreamCodec<ByteBuf, ToggleBurstPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
