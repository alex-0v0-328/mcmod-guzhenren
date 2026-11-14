package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent.Change;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 发给蓄力中客户端的服务端通知：蓄力蛊[Store Strength Gu]蓄力已满，或被服务端取消（{@link Change}）。
 * 由 {@code net.alex.guzhenren.network.StoreStrengthNotices} 发送；客户端未在蓄力时忽略。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record StoreStrengthNoticePayload(Change change) implements CustomPacketPayload {

    public static final Type<StoreStrengthNoticePayload> TYPE = new Type<>(Guzhenren.id("store_strength_notice"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(Change.class), StoreStrengthNoticePayload::change,
            StoreStrengthNoticePayload::new);

    @Override
    public @NotNull Type<StoreStrengthNoticePayload> type() { return TYPE; }
}
