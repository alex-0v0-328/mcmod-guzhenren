package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 操作主手元老蛊[Primeval Elder Gu]的客户端意图载荷，支持点按存入或通过长按滑条取出指定组数（存入时 {@link #stacks} 为
 * 0）；客户端滑条上限仅作界面可见参考，所有条件与数量由服务端
 * {@link net.alex.guzhenren.network.ClientIntents#primevalElder} 重新校验。
 */

public record PrimevalElderPayload(PrimevalElderAction action, int stacks) implements CustomPacketPayload {

    public static final Type<PrimevalElderPayload> TYPE = new Type<>(Guzhenren.id("primeval_elder"));
    public static final StreamCodec<FriendlyByteBuf, PrimevalElderPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(PrimevalElderAction.class), PrimevalElderPayload::action,
            ByteBufCodecs.VAR_INT, PrimevalElderPayload::stacks,
            PrimevalElderPayload::new);

    @Override
    public @NotNull Type<PrimevalElderPayload> type() { return TYPE; }
}
