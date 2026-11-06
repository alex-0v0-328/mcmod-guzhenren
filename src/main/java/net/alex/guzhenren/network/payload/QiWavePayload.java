package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one Strength Qi Gu [力气蛊] punch thrown as a Qi Wave [气波]: the id of the target
 * the client aimed at. {@code QiWaveService.fire} checks the buff, the empty hand and the target again.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService
 * @since 1.0.0
 */

public record QiWavePayload(int target) implements CustomPacketPayload {

    public static final Type<QiWavePayload> TYPE = new Type<>(Guzhenren.id("qi_wave"));
    public static final StreamCodec<FriendlyByteBuf, QiWavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QiWavePayload::target,
            QiWavePayload::new);

    @Override
    public @NotNull Type<QiWavePayload> type() { return TYPE; }
}
