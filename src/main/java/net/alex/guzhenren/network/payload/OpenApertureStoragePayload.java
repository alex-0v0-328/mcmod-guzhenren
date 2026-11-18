package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 请求打开指定空窍存储菜单（{@link ApertureStorageMenu}）的客户端意图载荷，仅携带目标空窍序号；服务端经
 * {@link net.alex.guzhenren.network.ModPayloads} 接收并重查序号有效性与非宝黄天[Treasure Yellow Heaven]限制，严禁在任意
 * 上行意图载荷中携带玩家数据。
 */

public record OpenApertureStoragePayload(int aperture) implements CustomPacketPayload {

    public static final Type<OpenApertureStoragePayload> TYPE = new Type<>(Guzhenren.id("open_aperture_storage"));
    public static final StreamCodec<FriendlyByteBuf, OpenApertureStoragePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenApertureStoragePayload::aperture,
            OpenApertureStoragePayload::new);

    @Override
    public @NotNull Type<OpenApertureStoragePayload> type() { return TYPE; }
}
