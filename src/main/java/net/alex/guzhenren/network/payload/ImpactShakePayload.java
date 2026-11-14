package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端通知：本地玩家附近有重击，镜头起始晃动 {@link #strength} 度，{@link #ticks} 内衰减。
 * 服务端已按观察者距离缩放强度（{@code net.alex.guzhenren.network.ImpactShakeEvents}）；客户端两值都钳制后用。
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
