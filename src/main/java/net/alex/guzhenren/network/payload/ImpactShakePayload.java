package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Server notice: something hit the world hard near this client's player, so its camera shakes {@link #strength}
 * degrees at the start, fading out over {@link #ticks}. The server already scaled the strength by the viewer's
 * distance ({@code net.alex.guzhenren.network.ImpactShakeEvents}); the client clamps both before use.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record ImpactShakePayload(float strength, int ticks) implements CustomPacketPayload {

    public static final Type<ImpactShakePayload> TYPE = new Type<>(Guzhenren.id("impact_shake"));
    public static final StreamCodec<FriendlyByteBuf, ImpactShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ImpactShakePayload::strength,
            ByteBufCodecs.VAR_INT, ImpactShakePayload::ticks,
            ImpactShakePayload::new);

    @Override
    public @NotNull Type<ImpactShakePayload> type() { return TYPE; }
}
