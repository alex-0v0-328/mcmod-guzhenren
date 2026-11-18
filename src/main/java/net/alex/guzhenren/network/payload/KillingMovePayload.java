package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 催动主手刃类蛊及可选杀招[Killing Move]的客户端意图载荷（配合蛊由服务端自动挑选），携带轮盘选中的杀招 ID（点按普攻时为
 * {@code KillingMoveService.PLAIN}）与松手时锁定目标的实体 ID（无目标时为 {@code KillingMoveService.NO_TARGET}）；客户
 * 端轮盘仅展示可用项，服务端由 {@link ClientIntents#killingMove} 重查目标存在性、存活状态、前方朝向及距离等全部条件。
 */

public record KillingMovePayload(String move, int target) implements CustomPacketPayload {

    public static final int MAX_ID_LENGTH = 64;
    public static final Type<KillingMovePayload> TYPE = new Type<>(Guzhenren.id("killing_move"));
    public static final StreamCodec<FriendlyByteBuf, KillingMovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), KillingMovePayload::move,
            ByteBufCodecs.INT, KillingMovePayload::target,
            KillingMovePayload::new);

    @Override
    public @NotNull Type<KillingMovePayload> type() { return TYPE; }
}
