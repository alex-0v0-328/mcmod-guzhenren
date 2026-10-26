package net.alex.guzhenren.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
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
import org.jetbrains.annotations.NotNull;

/**
 * Registers the seven client-intent payloads and forwards each to {@link ClientIntents}, which holds every
 * check, and the one server notice ({@code SkinFlashPayload}), handed to whatever the client registered for its
 * class through {@link #handleOnClient}.
 *
 * <p>⚠ A version seam: the intent records in {@code network.payload} are plain data with their own
 * {@code encode}/{@code decode}, the same on both loaders. NeoForge sends only a {@link CustomPacketPayload}, so
 * each record travels inside a {@link Carrier} whose type is registered here; {@link #wrap} finds it by the
 * record's class. The Forge 1.20.1 port registers the same records on its {@code SimpleChannel} instead.
 *
 * <p>⚠ A notice's client handler is looked up, never named here: this class loads on the server, and code the
 * server may load references nothing in {@code client}. {@code ClientEvents} registers the handlers at client
 * setup; a notice with none registered is dropped.
 *
 * @author Alex
 * @version 1.0.0
 * @see ClientIntents
 * @see ModNetwork
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModPayloads {

    private ModPayloads() {}

    private static final String VERSION = "1";
    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new HashMap<>();
    private static final Map<Class<?>, Consumer<?>> CLIENT_HANDLERS = new HashMap<>();

    /** One intent record on the wire, under the type registered for its class. */
    public record Carrier<T>(@NotNull Type<Carrier<T>> type, @NotNull T intent) implements CustomPacketPayload {}

    @SubscribeEvent
    public static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        toServer(registrar, "open_aperture_storage", OpenApertureStoragePayload.class,
                OpenApertureStoragePayload::encode, OpenApertureStoragePayload::decode,
                (intent, player) -> ClientIntents.openApertureStorage(player, intent.aperture()));
        toServer(registrar, "set_secondary_path", SetSecondaryPathPayload.class,
                SetSecondaryPathPayload::encode, SetSecondaryPathPayload::decode,
                (intent, player) -> ClientIntents.setSecondaryPath(player, intent.aperture(), intent.path()));
        toServer(registrar, "open_refinement", OpenRefinementPayload.class,
                (buf, intent) -> {}, buf -> OpenRefinementPayload.INSTANCE,
                (intent, player) -> ClientIntents.openRefinement(player));
        toServer(registrar, "nourish_aperture", NourishAperturePayload.class,
                NourishAperturePayload::encode, NourishAperturePayload::decode,
                (intent, player) -> ClientIntents.nourishAperture(player, intent.action(), intent.aperture()));
        toServer(registrar, "impact_aperture_wall", ImpactApertureWallPayload.class,
                (buf, intent) -> {}, buf -> ImpactApertureWallPayload.INSTANCE,
                (intent, player) -> ClientIntents.impactApertureWall(player));
        toServer(registrar, "dash", DashPayload.class,
                DashPayload::encode, DashPayload::decode,
                (intent, player) -> ClientIntents.dash(player, intent.vertical(), intent.horizontal(), intent.yRot()));
        toServer(registrar, "toggle_second_stage", ToggleSecondStagePayload.class,
                (buf, intent) -> {}, buf -> ToggleSecondStagePayload.INSTANCE,
                (intent, player) -> ClientIntents.toggleSecondStage(player));
        toClient(registrar, "skin_flash", SkinFlashPayload.class, SkinFlashPayload::encode, SkinFlashPayload::decode);
    }

    /** Registers what the client does with a notice of {@code noticeClass}; called from client setup only. */
    public static <T> void handleOnClient(Class<T> noticeClass, Consumer<T> handler) {
        CLIENT_HANDLERS.put(noticeClass, handler);
    }

    private static <T> void toClient(PayloadRegistrar registrar, String id, Class<T> noticeClass,
                                     BiConsumer<FriendlyByteBuf, T> encode, Function<FriendlyByteBuf, T> decode) {
        CustomPacketPayload.Type<Carrier<T>> type = new CustomPacketPayload.Type<>(Guzhenren.id(id));
        TYPES.put(noticeClass, type);
        registrar.playToClient(type,
                StreamCodec.<FriendlyByteBuf, Carrier<T>>of((buf, carrier) -> encode.accept(buf, carrier.intent()),
                        buf -> new Carrier<>(type, decode.apply(buf))),
                (carrier, context) -> dispatchToClient(noticeClass, carrier.intent()));
    }

    @SuppressWarnings("unchecked")
    private static <T> void dispatchToClient(Class<T> noticeClass, T notice) {
        Consumer<T> handler = (Consumer<T>) CLIENT_HANDLERS.get(noticeClass);
        if (handler != null) handler.accept(notice);
    }

    private static <T> void toServer(PayloadRegistrar registrar, String id, Class<T> intentClass,
                                     BiConsumer<FriendlyByteBuf, T> encode, Function<FriendlyByteBuf, T> decode,
                                     BiConsumer<T, ServerPlayer> handle) {
        CustomPacketPayload.Type<Carrier<T>> type = new CustomPacketPayload.Type<>(Guzhenren.id(id));
        TYPES.put(intentClass, type);
        registrar.playToServer(type,
                StreamCodec.<FriendlyByteBuf, Carrier<T>>of((buf, carrier) -> encode.accept(buf, carrier.intent()),
                        buf -> new Carrier<>(type, decode.apply(buf))),
                (carrier, context) -> {
                    if (context.player() instanceof ServerPlayer player) handle.accept(carrier.intent(), player);
                });
    }

    @SuppressWarnings("unchecked")
    static <T> CustomPacketPayload wrap(@NotNull T intent) {
        CustomPacketPayload.Type<Carrier<T>> type = (CustomPacketPayload.Type<Carrier<T>>) TYPES.get(intent.getClass());
        if (type == null) throw new IllegalArgumentException("not a registered intent: " + intent.getClass());
        return new Carrier<>(type, intent);
    }
}
