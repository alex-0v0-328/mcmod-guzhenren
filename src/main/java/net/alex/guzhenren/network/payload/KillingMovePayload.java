package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 主手月刃类蛊单次催动的客户端意图 payload；配合蛊由服务端挑选。
 * <ul>
 *   <li>{@code move}：轮盘所选杀招[Killing Move] id；点按为 {@code KillingMoveService.PLAIN}。</li>
 *   <li>{@code target}：松手时锁定目标的实体 id，无则为 {@code KillingMoveService.NO_TARGET}。</li>
 *   <li>{@link ClientIntents#killingMove} 全部重查，含目标存在、存活、在前方且在距离内；轮盘只列可通过项。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.killingmove.KillingMoveService
 * @since 1.0.0
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
