package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 双击疾跑进入或退出二段飞行[Second-Stage Flight]的客户端意图 payload；不带数据，无从伪造。
 * {@link net.alex.guzhenren.gameplay.flight.SecondStageService#toggle} 只按服务端自身状态判定；不能进入者保持原状。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.SecondStageService
 * @since 1.0.0
 */

public record ToggleSecondStagePayload() implements CustomPacketPayload {

    public static final ToggleSecondStagePayload INSTANCE = new ToggleSecondStagePayload();
    public static final Type<ToggleSecondStagePayload> TYPE = new Type<>(Guzhenren.id("toggle_second_stage"));
    public static final StreamCodec<FriendlyByteBuf, ToggleSecondStagePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ToggleSecondStagePayload> type() { return TYPE; }
}
