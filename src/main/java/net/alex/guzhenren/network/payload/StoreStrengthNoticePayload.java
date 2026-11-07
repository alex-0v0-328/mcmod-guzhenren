package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent.Change;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Server notice to the charging client: its Store Strength Gu [蓄力蛊] charge [蓄力] is now full, or the server
 * dropped it ({@link Change}). Sent by {@code net.alex.guzhenren.network.StoreStrengthNotices}; the client ignores
 * it when it is not charging.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record StoreStrengthNoticePayload(Change change) implements CustomPacketPayload {

    public static final Type<StoreStrengthNoticePayload> TYPE = new Type<>(Guzhenren.id("store_strength_notice"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(Change.class), StoreStrengthNoticePayload::change,
            StoreStrengthNoticePayload::new);

    @Override
    public @NotNull Type<StoreStrengthNoticePayload> type() { return TYPE; }
}
