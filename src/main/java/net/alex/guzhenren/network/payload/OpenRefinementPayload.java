package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 请求打开炼蛊界面（{@link RefinementMenu}）的零字节单例客户端意图载荷，仅表示一次点击触发而不携带任何玩家数据，经
 * {@link net.alex.guzhenren.network.ModPayloads} 分发至服务端校验开启。
 */

public record OpenRefinementPayload() implements CustomPacketPayload {

    public static final OpenRefinementPayload INSTANCE = new OpenRefinementPayload();
    public static final Type<OpenRefinementPayload> TYPE = new Type<>(Guzhenren.id("open_refinement"));
    public static final StreamCodec<FriendlyByteBuf, OpenRefinementPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<OpenRefinementPayload> type() { return TYPE; }
}
