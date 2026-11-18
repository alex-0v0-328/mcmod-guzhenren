package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 为指定空窍设置辅修流派的客户端意图载荷，仅携带空窍序号与可空流派 {@link GuPath}（通过
 * {@link ModByteBufs#nullableEnumCodec} 以序数加一编码、0 表示未设）而由 {@link net.alex.guzhenren.network.ModPayloads}
 * 交由空窍服务写入。
 * <ul>
 *   <li>⚠ 仅因设置界面背后未绑定原版容器才单独定义为网络载荷；凡容器菜单内的交互操作一律走原版容器同步通道。</li>
 * </ul>
 */

public record SetSecondaryPathPayload(int aperture, @Nullable GuPath path) implements CustomPacketPayload {

    public static final Type<SetSecondaryPathPayload> TYPE = new Type<>(Guzhenren.id("set_secondary_path"));
    public static final StreamCodec<FriendlyByteBuf, SetSecondaryPathPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetSecondaryPathPayload::aperture,
            ModByteBufs.nullableEnumCodec(GuPath.class), SetSecondaryPathPayload::path,
            SetSecondaryPathPayload::new);

    @Override
    public @NotNull Type<SetSecondaryPathPayload> type() { return TYPE; }
}
