package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Client intent: flush the aperture wall [冲刷窍壁] and take whatever the roll gives.
 *
 * <p>A zero-byte singleton payload -- it carries no data at all, only the button press. The server
 * handler in {@link net.alex.guzhenren.network.ModPayloads} delegates to
 * {@link ApertureNourishService}. Client intent is the one
 * direction attachment sync cannot carry; no player data travels upstream.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record ImpactApertureWallPayload() implements CustomPacketPayload {

    public static final ImpactApertureWallPayload INSTANCE = new ImpactApertureWallPayload();
    public static final Type<ImpactApertureWallPayload> TYPE = new Type<>(Guzhenren.id("impact_aperture_wall"));
    public static final StreamCodec<FriendlyByteBuf, ImpactApertureWallPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ImpactApertureWallPayload> type() { return TYPE; }
}
