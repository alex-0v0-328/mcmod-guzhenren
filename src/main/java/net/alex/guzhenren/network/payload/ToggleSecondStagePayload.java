package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 双击疾跑切换二段飞行[Second-Stage Flight]开关的零字节单例客户端意图载荷；由于不附带任何参数因而无法伪造，服务端
 * {@link net.alex.guzhenren.gameplay.flight.SecondStageService#toggle} 仅依据自身状态判定，不满足条件时保持原状。
 */

public record ToggleSecondStagePayload() implements CustomPacketPayload {

    public static final ToggleSecondStagePayload INSTANCE = new ToggleSecondStagePayload();
    public static final Type<ToggleSecondStagePayload> TYPE = new Type<>(Guzhenren.id("toggle_second_stage"));
    public static final StreamCodec<FriendlyByteBuf, ToggleSecondStagePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ToggleSecondStagePayload> type() { return TYPE; }
}
