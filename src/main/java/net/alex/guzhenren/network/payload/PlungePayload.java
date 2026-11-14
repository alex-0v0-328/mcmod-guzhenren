package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 空中潜行加 Alt 发起坠击的客户端意图 payload；不带数据，无从伪造。
 * {@link net.alex.guzhenren.gameplay.plunge.PlungeService#start} 只按服务端自身状态判定；不能坠击者照常下落或飞行。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.plunge.PlungeService
 * @since 1.0.0
 */

public record PlungePayload() implements CustomPacketPayload {

    public static final PlungePayload INSTANCE = new PlungePayload();
    public static final Type<PlungePayload> TYPE = new Type<>(Guzhenren.id("plunge"));
    public static final StreamCodec<FriendlyByteBuf, PlungePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<PlungePayload> type() { return TYPE; }
}
