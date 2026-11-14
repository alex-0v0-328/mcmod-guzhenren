package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 打开指定空窍[Aperture]存储的客户端意图，只带空窍序号。
 * <ul>
 *   <li>由 {@link net.alex.guzhenren.network.ModPayloads} 处理，打开 {@link ApertureStorageMenu}。</li>
 *   <li>⚠ 任何 payload 都不得带玩家数据，它只作同步状态反向下发；附件同步唯独不承载客户端意图。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record OpenApertureStoragePayload(int aperture) implements CustomPacketPayload {

    public static final Type<OpenApertureStoragePayload> TYPE = new Type<>(Guzhenren.id("open_aperture_storage"));
    public static final StreamCodec<FriendlyByteBuf, OpenApertureStoragePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenApertureStoragePayload::aperture,
            OpenApertureStoragePayload::new);

    @Override
    public @NotNull Type<OpenApertureStoragePayload> type() { return TYPE; }
}
