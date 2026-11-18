package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 空中潜行加 Alt 发起坠击的零字节单例客户端意图载荷；由于不附带任何参数因而无法伪造，服务端
 * {@link net.alex.guzhenren.gameplay.plunge.PlungeService#start} 仅依据自身状态判定，不满足条件时玩家维持正常下落或飞
 * 行。
 */

public record PlungePayload() implements CustomPacketPayload {

    public static final PlungePayload INSTANCE = new PlungePayload();
    public static final Type<PlungePayload> TYPE = new Type<>(Guzhenren.id("plunge"));
    public static final StreamCodec<FriendlyByteBuf, PlungePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<PlungePayload> type() { return TYPE; }
}
