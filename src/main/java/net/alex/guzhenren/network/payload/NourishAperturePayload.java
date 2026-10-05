package net.alex.guzhenren.network.payload;

import io.netty.buffer.ByteBuf;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModStreamCodecs;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Client intent: begin or abandon nourishing the aperture [温养空窍]. A payload carrying a named
 * action ({@code START} / {@code CANCEL}) and the target aperture index -- no player data. The server
 * handler in {@link net.alex.guzhenren.network.ModPayloads} delegates to {@link
 * ApertureNourishService}; client intent is the one
 * direction attachment sync cannot carry.
 *
 * <p>⚠ Two intents ride one payload as a named action rather than a bare boolean, because a boolean at
 * the call site reads as nothing at all.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record NourishAperturePayload(Action action, int aperture) implements CustomPacketPayload {

    public enum Action { START, CANCEL }

    public static final Type<NourishAperturePayload> TYPE = new Type<>(
            Guzhenren.id("nourish_aperture"));
    public static final StreamCodec<ByteBuf, NourishAperturePayload> STREAM_CODEC = StreamCodec.composite(
            ModStreamCodecs.ofEnum(Action.class), NourishAperturePayload::action,
            ByteBufCodecs.VAR_INT, NourishAperturePayload::aperture,
            NourishAperturePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
