package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 功倍蛊[Amplify Effect Gu]轮盘单选后触发目标蛊虫催动的客户端意图载荷，仅携带目标快捷栏槽位索引；客户端轮盘仅展示可用
 * 项，服务端由 {@link ClientIntents#amplifyDrive} 对所有前置条件执行完整重查。
 */

public record AmplifyDrivePayload(int slot) implements CustomPacketPayload {

    public static final Type<AmplifyDrivePayload> TYPE = new Type<>(Guzhenren.id("amplify_drive"));
    public static final StreamCodec<FriendlyByteBuf, AmplifyDrivePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, AmplifyDrivePayload::slot, AmplifyDrivePayload::new);

    @Override
    public @NotNull Type<AmplifyDrivePayload> type() { return TYPE; }
}
