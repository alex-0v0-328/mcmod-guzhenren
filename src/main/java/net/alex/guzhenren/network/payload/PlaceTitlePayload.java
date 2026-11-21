package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 下发给玩家客户端的地名标题[Place Title]通知载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，
 * {@code PlaceTitleNotices} 发送）：类别与地名的语言键、地名颜色（RGB）。
 */

public record PlaceTitlePayload(String kindKey, String nameKey, int color) implements CustomPacketPayload {

    public static final Type<PlaceTitlePayload> TYPE = new Type<>(Guzhenren.id("place_title"));
    public static final StreamCodec<FriendlyByteBuf, PlaceTitlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlaceTitlePayload::kindKey,
            ByteBufCodecs.STRING_UTF8, PlaceTitlePayload::nameKey,
            ByteBufCodecs.INT, PlaceTitlePayload::color,
            PlaceTitlePayload::new);

    @Override
    public @NotNull Type<PlaceTitlePayload> type() { return TYPE; }
}
