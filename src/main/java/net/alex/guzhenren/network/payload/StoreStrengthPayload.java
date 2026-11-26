package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu]蓄力交互的客户端意图载荷，携带 {@link StoreStrengthAction} 与锁定目标实体 ID（无目标时为
 * {@link #NO_TARGET}），经 {@link net.alex.guzhenren.network.ClientIntents#storeStrength} 转交
 * {@link net.alex.guzhenren.gameplay.path.strength.StoreStrength} 重查增益、力道、空手、蓄力状态与目标合法性。
 */

public record StoreStrengthPayload(StoreStrengthAction action, int target) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;
    public static final Type<StoreStrengthPayload> TYPE = new Type<>(Guzhenren.id("store_strength"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(StoreStrengthAction.class), StoreStrengthPayload::action,
            ByteBufCodecs.VAR_INT, StoreStrengthPayload::target,
            StoreStrengthPayload::new);

    @Override
    public @NotNull Type<StoreStrengthPayload> type() { return TYPE; }
}
