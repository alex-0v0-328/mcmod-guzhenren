package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 请求执行一次冲刷窍壁的零字节单例客户端意图载荷，仅表示按键触发（结果完全由服务端掷骰决定）；由
 * {@link net.alex.guzhenren.network.ModPayloads} 分发至 {@link ApertureNourishService}，不附带任何玩家附件数据。
 */

public record ImpactApertureWallPayload() implements CustomPacketPayload {

    public static final ImpactApertureWallPayload INSTANCE = new ImpactApertureWallPayload();
    public static final Type<ImpactApertureWallPayload> TYPE = new Type<>(Guzhenren.id("impact_aperture_wall"));
    public static final StreamCodec<FriendlyByteBuf, ImpactApertureWallPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ImpactApertureWallPayload> type() { return TYPE; }
}
