package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 单次冲刺的客户端意图 payload：竖直、水平方向及取向时的朝向；对应蛊效果生效期间发送。
 * <ul>
 *   <li>{@link ClientIntents#canDash} 是唯一冲刺规则，两端共读：客户端发送前、服务端移动玩家前各查一次。</li>
 *   <li>水平需横冲蛊[Horizontal Crash Gu]，竖直需直撞蛊[Vertical Crash Gu]，横冲直撞蛊[Charging Crash Gu]皆可。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.CrashGuEffect
 * @since 1.0.0
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
