package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one use of a killer move's [杀招] main Gu from the radial menu: the hotbar slot of
 * the partner picked, or {@code KillerMoveService.NONE} for a plain use. {@link ClientIntents#killerMove} checks it
 * all again; the client's menu only lists what it would pass.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.killermove.KillerMoveService
 * @since 1.0.0
 */

public record KillerMovePayload(int slot) implements CustomPacketPayload {

    public static final Type<KillerMovePayload> TYPE = new Type<>(Guzhenren.id("killer_move"));
    public static final StreamCodec<FriendlyByteBuf, KillerMovePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, KillerMovePayload::slot, KillerMovePayload::new);

    @Override
    public @NotNull Type<KillerMovePayload> type() { return TYPE; }
}
