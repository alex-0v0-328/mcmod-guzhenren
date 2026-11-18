package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 力气蛊[Strength Qi Gu]出拳化作气波时的客户端意图载荷，仅携带客户端瞄准目标的实体 ID，服务端在
 * {@link net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService#fire} 中重新校验效果增益、空手状态与目标合法性。
 */

public record QiWavePayload(int target) implements CustomPacketPayload {

    public static final Type<QiWavePayload> TYPE = new Type<>(Guzhenren.id("qi_wave"));
    public static final StreamCodec<FriendlyByteBuf, QiWavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QiWavePayload::target,
            QiWavePayload::new);

    @Override
    public @NotNull Type<QiWavePayload> type() { return TYPE; }
}
