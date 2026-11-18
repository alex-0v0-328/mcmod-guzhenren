package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent.Change;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 下发给正在蓄力客户端的蓄力蛊[Store Strength Gu]状态通知载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，
 * {@code StoreStrengthNotices} 发送），携带已满或被服务端取消的 {@link Change} 枚举；客户端未处于蓄力态时直接忽略。
 */

public record StoreStrengthNoticePayload(Change change) implements CustomPacketPayload {

    public static final Type<StoreStrengthNoticePayload> TYPE = new Type<>(Guzhenren.id("store_strength_notice"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(Change.class), StoreStrengthNoticePayload::change,
            StoreStrengthNoticePayload::new);

    @Override
    public @NotNull Type<StoreStrengthNoticePayload> type() { return TYPE; }
}
