package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for sneak plus Alt in the air: start a plunge [坠击].
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.plunge.PlungeService#start} decides from the server's own state, and a player
 * who may not plunge simply keeps falling or flying.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.plunge.PlungeService
 * @since 1.0.0
 */

public record PlungePayload() implements CustomPacketPayload {

    public static final PlungePayload INSTANCE = new PlungePayload();
    public static final Type<PlungePayload> TYPE = new Type<>(Guzhenren.id("plunge"));
    public static final StreamCodec<FriendlyByteBuf, PlungePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<PlungePayload> type() { return TYPE; }
}
