package net.alex.guzhenren.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.network.payload.ToggleBurstPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Registers the seven client-intent payloads and forwards each to {@link ClientIntents}, which holds every
 * check.
 *
 * <p>⚠ A version seam: the intent records in {@code network.payload} are plain data with their own
 * {@code encode}/{@code decode}, the same on both loaders. Here they register straight onto {@link
 * ModNetwork#CHANNEL}, handled on the server thread; the NeoForge 1.21.1 mainline wraps each in a registered
 * payload carrier instead.
 *
 * <p>⚠ Message ids follow registration order and continue after {@link ModNetwork}'s own packets: append new
 * intents at the end.
 *
 * @author Alex
 * @version 1.0.0
 * @see ClientIntents
 * @see ModNetwork
 * @since 1.0.0
 */

public final class ModPayloads {

    private ModPayloads() {}

    /** Registers every intent from {@code firstId} on; returns the next free id. */
    static int register(SimpleChannel channel, int firstId) {
        int id = firstId;
        id = toServer(channel, id, OpenApertureStoragePayload.class,
                OpenApertureStoragePayload::encode, OpenApertureStoragePayload::decode,
                (intent, player) -> ClientIntents.openApertureStorage(player, intent.aperture()));
        id = toServer(channel, id, SetSecondaryPathPayload.class,
                SetSecondaryPathPayload::encode, SetSecondaryPathPayload::decode,
                (intent, player) -> ClientIntents.setSecondaryPath(player, intent.aperture(), intent.path()));
        id = toServer(channel, id, OpenRefinementPayload.class,
                (buf, intent) -> {}, buf -> OpenRefinementPayload.INSTANCE,
                (intent, player) -> ClientIntents.openRefinement(player));
        id = toServer(channel, id, NourishAperturePayload.class,
                NourishAperturePayload::encode, NourishAperturePayload::decode,
                (intent, player) -> ClientIntents.nourishAperture(player, intent.action(), intent.aperture()));
        id = toServer(channel, id, ImpactApertureWallPayload.class,
                (buf, intent) -> {}, buf -> ImpactApertureWallPayload.INSTANCE,
                (intent, player) -> ClientIntents.impactApertureWall(player));
        id = toServer(channel, id, DashPayload.class,
                DashPayload::encode, DashPayload::decode,
                (intent, player) -> ClientIntents.dash(player, intent.vertical(), intent.horizontal(), intent.yRot()));
        id = toServer(channel, id, ToggleBurstPayload.class,
                (buf, intent) -> {}, buf -> ToggleBurstPayload.INSTANCE,
                (intent, player) -> ClientIntents.toggleBurst(player));
        return id;
    }

    private static <T> int toServer(SimpleChannel channel, int id, Class<T> intentClass,
                                    BiConsumer<FriendlyByteBuf, T> encode, Function<FriendlyByteBuf, T> decode,
                                    BiConsumer<T, ServerPlayer> handle) {
        channel.messageBuilder(intentClass, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder((intent, buf) -> encode.accept(buf, intent))
                .decoder(decode)
                .consumerMainThread((intent, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) handle.accept(intent, player);
                })
                .add();
        return id + 1;
    }
}
