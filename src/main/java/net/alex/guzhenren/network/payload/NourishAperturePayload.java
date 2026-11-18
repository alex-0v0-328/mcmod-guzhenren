package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 请求开始或取消指定空窍温养[Nourish]的客户端意图载荷，携带具名动作 {@link NourishAction} 与空窍序号而不附带玩家数据，
 * 经 {@link net.alex.guzhenren.network.ModPayloads} 与 {@link net.alex.guzhenren.network.ClientIntents} 转交
 * {@link ApertureNourishService} 处理。
 * <ul>
 *   <li>⚠ 开始与取消两种意图刻意共用同一载荷并采用具名枚举而非裸布尔参数，避免调用点出现语义不明的布尔字面量。</li>
 * </ul>
 */

public record NourishAperturePayload(NourishAction action, int aperture) implements CustomPacketPayload {

    public static final Type<NourishAperturePayload> TYPE = new Type<>(Guzhenren.id("nourish_aperture"));
    public static final StreamCodec<FriendlyByteBuf, NourishAperturePayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(NourishAction.class), NourishAperturePayload::action,
            ByteBufCodecs.VAR_INT, NourishAperturePayload::aperture,
            NourishAperturePayload::new);

    @Override
    public @NotNull Type<NourishAperturePayload> type() { return TYPE; }
}
