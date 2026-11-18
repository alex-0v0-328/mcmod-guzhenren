package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 通知本地玩家附近发生重击的服务端下行载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，
 * {@code ImpactShakeEvents} 按观察者距离预先衰减强度），指示客户端以起始强度 {@link #strength} 度、持续 {@link #ticks}
 * 刻播放镜头晃动（客户端使用前会对两值做安全钳制）。
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
