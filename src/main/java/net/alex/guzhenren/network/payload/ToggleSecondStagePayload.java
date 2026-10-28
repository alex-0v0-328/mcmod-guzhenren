package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for a sprint double tap: enter second-stage flight [二段飞行], or leave it.
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.flight.SecondStageService#toggle} decides from the server's own state, and a
 * player who may not enter simply stays out.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.SecondStageService
 * @since 1.0.0
 */

public record ToggleSecondStagePayload() implements CustomPacketPayload {

    public static final ToggleSecondStagePayload INSTANCE = new ToggleSecondStagePayload();
    public static final Type<ToggleSecondStagePayload> TYPE = new Type<>(Guzhenren.id("toggle_second_stage"));
    public static final StreamCodec<FriendlyByteBuf, ToggleSecondStagePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ToggleSecondStagePayload> type() { return TYPE; }
}
