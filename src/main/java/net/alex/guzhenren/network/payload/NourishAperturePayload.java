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
 * 开始或放弃温养[Nourish]空窍[Aperture]的客户端意图：具名动作（{@link NourishAction}）加空窍序号，不带玩家数据。
 * <ul>
 *   <li>由 {@link net.alex.guzhenren.network.ModPayloads} 处理，经 {@link net.alex.guzhenren.network.ClientIntents}
 *       转交 {@link ApertureNourishService}；附件同步唯独不承载客户端意图。</li>
 *   <li>⚠ 两种意图共用一个 payload，用具名动作而非裸布尔：调用处布尔看不出含义。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
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
