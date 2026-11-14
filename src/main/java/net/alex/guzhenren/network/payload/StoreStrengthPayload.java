package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu]蓄力的客户端意图 payload：开始、取消，或对锁定目标 id 释放。
 * {@link net.alex.guzhenren.network.ClientIntents#storeStrength} 转交 {@code StoreStrength}，重查增益、力道、拳头、
 * 蓄力与目标。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.StoreStrength
 * @since 1.0.0
 */

public record StoreStrengthPayload(ChargeAction action, int target) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;
    public static final Type<StoreStrengthPayload> TYPE = new Type<>(Guzhenren.id("store_strength"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(ChargeAction.class), StoreStrengthPayload::action,
            ByteBufCodecs.VAR_INT, StoreStrengthPayload::target,
            StoreStrengthPayload::new);

    @Override
    public @NotNull Type<StoreStrengthPayload> type() { return TYPE; }
}
