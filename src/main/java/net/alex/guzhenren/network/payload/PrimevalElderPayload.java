package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 主手元老蛊[Primeval Elder Gu]的客户端意图 payload：点按存入，或长按滑条取出 {@code stacks} 组（存入时为 {@code 0}）。
 * {@link net.alex.guzhenren.network.ClientIntents#primevalElder} 全部重查；滑条上限只是客户端可见的数量。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem
 * @since 1.0.0
 */

public record PrimevalElderPayload(ElderAction action, int stacks) implements CustomPacketPayload {

    public static final Type<PrimevalElderPayload> TYPE = new Type<>(Guzhenren.id("primeval_elder"));
    public static final StreamCodec<FriendlyByteBuf, PrimevalElderPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(ElderAction.class), PrimevalElderPayload::action,
            ByteBufCodecs.VAR_INT, PrimevalElderPayload::stacks,
            PrimevalElderPayload::new);

    @Override
    public @NotNull Type<PrimevalElderPayload> type() { return TYPE; }
}
