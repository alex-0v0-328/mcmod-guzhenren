package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 冲撞类蛊效果生效期间发起单次冲刺的客户端意图载荷，携带竖直输入、水平输入与取向瞬间的模型朝向 {@code yRot}（共读
 * {@link ClientIntents#canDash} 作为双端唯一冲刺方向校验：横冲蛊[Horizontal Crash Gu]仅开放水平轴，
 * 直撞蛊[Vertical Crash Gu]仅开放竖直轴，横冲直撞蛊[Charging Crash Gu]同时开放两轴）。
 */

public record DashPayload(int vertical, int horizontal, float yRot) implements CustomPacketPayload {

    public static final Type<DashPayload> TYPE = new Type<>(Guzhenren.id("dash"));
    public static final StreamCodec<FriendlyByteBuf, DashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DashPayload::vertical,
            ByteBufCodecs.INT, DashPayload::horizontal,
            ByteBufCodecs.FLOAT, DashPayload::yRot,
            DashPayload::new);

    @Override
    public @NotNull Type<DashPayload> type() { return TYPE; }
}
