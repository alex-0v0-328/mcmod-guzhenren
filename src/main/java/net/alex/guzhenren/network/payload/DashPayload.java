package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one dash.
 *
 * <p>Sent by the client while a matching effect is running; carries the vertical and horizontal
 * directions plus the facing they were taken at. {@link ClientIntents#canDash} is the one dash rule, read by both
 * ends: the client checks it before sending, the server again before moving the player. Each axis moved needs
 * its Crash Gu effect -- the charging one covers both.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.CrashGuEffect
 * @since 1.0.0
 */

public record DashPayload(int vertical, int horizontal, float yRot) implements CustomPacketPayload {

    public static final Type<DashPayload> TYPE = new Type<>(Guzhenren.id("dash"));
    public static final StreamCodec<FriendlyByteBuf, DashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DashPayload::vertical,
            ByteBufCodecs.INT, DashPayload::horizontal,
            ByteBufCodecs.FLOAT, DashPayload::yRot,
            DashPayload::new);

    @Override
    public @NotNull Type<DashPayload> type() { return TYPE; }
}
