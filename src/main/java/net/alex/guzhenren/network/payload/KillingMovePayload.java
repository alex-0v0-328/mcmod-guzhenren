package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one use of a moon blade Gu [月刃] from the main hand: the id of the killing move
 * [杀招] picked on the radial menu, or {@code KillingMoveService.PLAIN} for a tap, and the entity id of the target
 * the client had locked on [锁定目标] when it let go, or {@link #NO_TARGET}. It names no partner: the server picks
 * one, and {@link ClientIntents#killingMove} checks it all again, the target included (it exists, is alive, is
 * ahead and within reach); the client's menu only lists what it would pass.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.killingmove.KillingMoveService
 * @since 1.0.0
 */

public record KillingMovePayload(String move, int target) implements CustomPacketPayload {

    public static final int MAX_ID_LENGTH = 64;
    public static final int NO_TARGET = -1;
    public static final Type<KillingMovePayload> TYPE = new Type<>(Guzhenren.id("killing_move"));
    public static final StreamCodec<FriendlyByteBuf, KillingMovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), KillingMovePayload::move,
            ByteBufCodecs.INT, KillingMovePayload::target,
            KillingMovePayload::new);

    @Override
    public @NotNull Type<KillingMovePayload> type() { return TYPE; }
}
