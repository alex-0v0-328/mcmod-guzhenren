package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.network.ClientIntents.ElderAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for the main-hand Primeval Elder Gu [元老蛊]: a tap's deposit, or the hold slider's
 * withdrawal of {@code stacks} stacks ({@code 0} for a deposit). {@link ClientIntents#primevalElder} checks it all
 * again; the slider's top is only what the client could see.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem
 * @since 1.0.0
 */

public record PrimevalElderPayload(ElderAction action, int stacks) implements CustomPacketPayload {

    public static final Type<PrimevalElderPayload> TYPE = new Type<>(Guzhenren.id("primeval_elder"));
    public static final StreamCodec<FriendlyByteBuf, PrimevalElderPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(ElderAction.class), PrimevalElderPayload::action,
            ByteBufCodecs.VAR_INT, PrimevalElderPayload::stacks,
            PrimevalElderPayload::new);

    @Override
    public @NotNull Type<PrimevalElderPayload> type() { return TYPE; }
}
