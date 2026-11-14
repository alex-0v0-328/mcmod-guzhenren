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
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.PlungePayload;
import net.alex.guzhenren.network.payload.PrimevalElderPayload;
import net.alex.guzhenren.network.payload.QiWavePayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
import net.alex.guzhenren.network.payload.StoreStrengthPayload;
import net.alex.guzhenren.network.payload.ToggleSecondStagePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络 payload 的注册与分发。
 * <ul>
 *   <li>11 个客户端意图 payload 转 {@link ClientIntents}；3 个服务端通知经 {@link #handleOnClient} 交给客户端按类登记的
 *       处理器：{@code SkinFlashPayload}、{@code ImpactShakePayload}、{@code KillingMoveTooltipPayload}。</li>
 *   <li>{@code network.payload} 每个 record 是独立的 {@link CustomPacketPayload}，自带 {@code TYPE}、
 *       {@code STREAM_CODEC}，经 NeoForge {@code PacketDistributor} 发送；处理器跑在主线程（NeoForge 默认）。</li>
 *   <li>⚠ 通知的客户端处理器靠查找、不在此点名：本类在服务端加载，服务端可能加载的代码不引用 {@code client}；
 *       {@code ClientEvents} 在客户端初始化时登记，未登记的通知丢弃。</li>
 * </ul>
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
                (intent, player) -> ClientIntents.killingMove(player, intent.move(), intent.target()));
        toServer(registrar, QiWavePayload.TYPE, QiWavePayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.qiWave(player, intent.target()));
        toServer(registrar, StoreStrengthPayload.TYPE, StoreStrengthPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.storeStrength(player, intent.action(), intent.target()));
        toServer(registrar, PrimevalElderPayload.TYPE, PrimevalElderPayload.STREAM_CODEC,
                (intent, player) -> ClientIntents.primevalElder(player, intent.action(), intent.stacks()));
        toClient(registrar, SkinFlashPayload.TYPE, SkinFlashPayload.STREAM_CODEC);
        toClient(registrar, ImpactShakePayload.TYPE, ImpactShakePayload.STREAM_CODEC);
        toClient(registrar, KillingMoveTooltipPayload.TYPE, KillingMoveTooltipPayload.STREAM_CODEC);
        toClient(registrar, StoreStrengthNoticePayload.TYPE, StoreStrengthNoticePayload.STREAM_CODEC);
    }

    /** 登记客户端如何处理 {@code noticeClass} 类通知；只在客户端初始化时调用。 */
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

    private static <T extends CustomPacketPayload> void toClient(
            PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        registrar.playToClient(type, codec, (notice, context) -> dispatchToClient(notice));
    }
}
