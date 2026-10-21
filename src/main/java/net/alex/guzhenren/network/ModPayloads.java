package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.network.payload.ToggleBurstPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the seven client-intent payloads and forwards each to {@link ClientIntents}, which holds every
 * check; this file and the payload records are the only loader-specific network code besides {@link ModNetwork}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ClientIntents
 * @see net.alex.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModPayloads {

    private ModPayloads() {}

    private static final String VERSION = "1";

    @SubscribeEvent
    public static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(OpenApertureStoragePayload.TYPE, OpenApertureStoragePayload.STREAM_CODEC,
                ModPayloads::openStorage);
        registrar.playToServer(SetSecondaryPathPayload.TYPE, SetSecondaryPathPayload.STREAM_CODEC,
                ModPayloads::setSecondaryPath);
        registrar.playToServer(OpenRefinementPayload.TYPE, OpenRefinementPayload.STREAM_CODEC,
                ModPayloads::openRefinement);
        registrar.playToServer(NourishAperturePayload.TYPE, NourishAperturePayload.STREAM_CODEC,
                ModPayloads::nourishAperture);
        registrar.playToServer(ImpactApertureWallPayload.TYPE, ImpactApertureWallPayload.STREAM_CODEC,
                ModPayloads::impactApertureWall);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC,
                ModPayloads::dash);
        registrar.playToServer(ToggleBurstPayload.TYPE, ToggleBurstPayload.STREAM_CODEC,
                ModPayloads::toggleBurst);
    }

    private static void nourishAperture(NourishAperturePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ClientIntents.nourishAperture(player, payload.action(), payload.aperture());
        }
    }

    private static void impactApertureWall(ImpactApertureWallPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) ClientIntents.impactApertureWall(player);
    }

    private static void dash(DashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ClientIntents.dash(player, payload.vertical(), payload.horizontal(), payload.yRot());
        }
    }

    private static void toggleBurst(ToggleBurstPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) ClientIntents.toggleBurst(player);
    }

    private static void openRefinement(OpenRefinementPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) ClientIntents.openRefinement(player);
    }

    private static void setSecondaryPath(SetSecondaryPathPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ClientIntents.setSecondaryPath(player, payload.aperture(), payload.path());
        }
    }

    private static void openStorage(OpenApertureStoragePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ClientIntents.openApertureStorage(player, payload.aperture());
        }
    }
}
