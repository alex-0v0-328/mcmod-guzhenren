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
 * 为指定空窍[Aperture]选辅修流派的客户端意图：只带空窍序号与可空 {@link GuPath}，无其他玩家数据。
 * <ul>
 *   <li>由 {@link net.alex.guzhenren.network.ModPayloads} 处理，经空窍服务写入。</li>
 *   <li>道[Path]以序数加一传送，0 为未设，见 {@link ModByteBufs#writeNullableEnum}。</li>
 *   <li>⚠ 只因发送界面背后无容器才做成 payload；容器内操作走原版自身通道。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
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
