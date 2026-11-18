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
 * 模组自定义网络载荷的集中注册与双端分发入口。
 * <ul>
 *   <li>全部载荷定义于 {@code network.payload} 子包中，各自实现 {@link CustomPacketPayload} 并声明 {@code TYPE} 与
 *       {@code STREAM_CODEC}，通过 NeoForge {@code PacketDistributor} 收发且默认在主线程执行；上行意图交由
 *       {@link ClientIntents} 处理，下行通知经 {@link #dispatchToClient} 按类路由至 {@link #handleOnClient} 登记的处理
 *       器。</li>
 *   <li>⚠ 客户端通知处理器通过运行时查表分派而不在此直接引用：本类会在专用服务端加载，任何服务端可加载类均不得引用
 *       {@code client} 包；处理器由客户端初始化时登记，未登记的通知将被静默丢弃。</li>
 *   <li>⚠ 修改任意载荷的字段结构或编解码顺序时必须同步递增 {@link #VERSION}，防止版本不匹配的双端混连。</li>
 * </ul>
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
