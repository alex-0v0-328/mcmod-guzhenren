package net.alex.guzhenren.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.payload.AmplifyDrivePayload;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.alex.guzhenren.network.payload.KillingMovePayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.PlungePayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.alex.guzhenren.network.payload.ToggleSecondStagePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the ten client-intent payloads and forwards each to {@link ClientIntents}, which holds every
 * check, and the two server notices ({@code SkinFlashPayload}, {@code ImpactShakePayload}), each handed to whatever
 * the client registered for its class through {@link #handleOnClient}.
 *
 * <p>Each record in {@code network.payload} is its own {@link CustomPacketPayload}, with its {@code TYPE} and
 * {@code STREAM_CODEC}; callers send them through NeoForge's {@code PacketDistributor}. The handlers run on the main
 * thread, NeoForge's default.
 *
 * <p>⚠ A notice's client handler is looked up, never named here: this class loads on the server, and code the
 * server may load references nothing in {@code client}. {@code ClientEvents} registers the handlers at client
 * setup; a notice with none registered is dropped.
 *
 * @author Alex
 * @version 1.0.0
 * @see ClientIntents
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModPayloads {

    private ModPayloads() {}

    private static final String VERSION = "1";
    private static final Map<Class<?>, Consumer<?>> CLIENT_HANDLERS = new HashMap<>();

    @SubscribeEvent
    public static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        toServer(registrar, OpenApertureStoragePayload.TYPE, OpenApertureStoragePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.openApertureStorage(player, intent.aperture()));
        toServer(registrar, SetSecondaryPathPayload.TYPE, SetSecondaryPathPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.setSecondaryPath(player, intent.aperture(), intent.path()));
        toServer(registrar, OpenRefinementPayload.TYPE, OpenRefinementPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.openRefinement(player));
        toServer(registrar, NourishAperturePayload.TYPE, NourishAperturePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.nourishAperture(player, intent.action(), intent.aperture()));
        toServer(registrar, ImpactApertureWallPayload.TYPE, ImpactApertureWallPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.impactApertureWall(player));
        toServer(registrar, DashPayload.TYPE, DashPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.dash(player, intent.vertical(), intent.horizontal(), intent.yRot()));
        toServer(registrar, ToggleSecondStagePayload.TYPE, ToggleSecondStagePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.toggleSecondStage(player));
        toServer(registrar, PlungePayload.TYPE, PlungePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.plunge(player));
        toServer(registrar, AmplifyDrivePayload.TYPE, AmplifyDrivePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.amplifyDrive(player, intent.slot()));
        toServer(registrar, KillingMovePayload.TYPE, KillingMovePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.killingMove(player, intent.slot()));
        registrar.playToClient(SkinFlashPayload.TYPE, SkinFlashPayload.STREAM_CODEC,
                (notice, context) -> dispatchToClient(notice));
        registrar.playToClient(ImpactShakePayload.TYPE, ImpactShakePayload.STREAM_CODEC,
                (notice, context) -> dispatchToClient(notice));
    }

    /** Registers what the client does with a notice of {@code noticeClass}; called from client setup only. */
    public static <T extends CustomPacketPayload> void handleOnClient(Class<T> noticeClass, Consumer<T> handler) {
        CLIENT_HANDLERS.put(noticeClass, handler);
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void dispatchToClient(T notice) {
        Consumer<T> handler = (Consumer<T>) CLIENT_HANDLERS.get(notice.getClass());
        if (handler != null) handler.accept(notice);
    }

    private static <T extends CustomPacketPayload> void toServer(PayloadRegistrar registrar,
                                                                 CustomPacketPayload.Type<T> type,
                                                                 StreamCodec<FriendlyByteBuf, T> codec,
                                                                 BiConsumer<T, ServerPlayer> handle) {
        registrar.playToServer(type, codec, (intent, context) -> {
            if (context.player() instanceof ServerPlayer player) handle.accept(intent, player);
        });
    }
}
