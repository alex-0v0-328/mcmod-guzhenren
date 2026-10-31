package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one use of a killing move's [杀招] main Gu from the radial menu: the hotbar slot of
 * the partner picked, or {@code KillingMoveService.NONE} for a plain use. {@link ClientIntents#killingMove} checks it
 * all again; the client's menu only lists what it would pass.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.killingmove.KillingMoveService
 * @since 1.0.0
 */

public record KillingMovePayload(int slot) implements CustomPacketPayload {

    public static final Type<KillingMovePayload> TYPE = new Type<>(Guzhenren.id("killing_move"));
    public static final StreamCodec<FriendlyByteBuf, KillingMovePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, KillingMovePayload::slot, KillingMovePayload::new);

    @Override
    public @NotNull Type<KillingMovePayload> type() { return TYPE; }
}
