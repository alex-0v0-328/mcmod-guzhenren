package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.gameplay.refinement.RefinementMenu;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the client-intent payloads and handles each of them on the server. Every payload in this
 * mod is a client intent -- a B-panel button or movement input that attachment sync cannot carry
 * upstream; none carries player data, and downstream player data always travels as synced state. This
 * class wires the six payloads to their server-side handlers: the two containers, the secondary path,
 * and the three cultivation actions.
 *
 * <p>⚠ This is where a forged payload lands, so a gate that only grays out a button is not a gate:
 * every refusal has to exist here as well as on the screen.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModPayloads {

    private ModPayloads() {}

    private static final String VERSION = "1";

    private static @Nullable ServerPlayer outsideTyh(IPayloadContext context) {
        return context.player() instanceof ServerPlayer player
                && !player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN) ? player : null;
    }

    private static boolean hasAperture(ServerPlayer player, int index) {
        return index >= 0 && index < ApertureService.get(player).count();
    }

    private static final String STORAGE_TITLE = "guzhenren.menu.aperture_storage";
    private static final String REFINEMENT_TITLE = "guzhenren.menu.refinement";

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
    }

    private static void nourishAperture(NourishAperturePayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        if (player == null || !hasAperture(player, payload.aperture())) return;

        switch (payload.action()) {
            case START -> ApertureNourishService.start(player, payload.aperture());
            case CANCEL -> ApertureNourishService.cancel(player);
        }
    }

    private static void impactApertureWall(ImpactApertureWallPayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        if (player != null) ApertureNourishService.impactWall(player);
    }

    private static void dash(DashPayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        if (player == null) return;

        int vertical = payload.vertical();
        int horizontal = payload.horizontal();
        if (vertical < -1 || vertical > 1 || horizontal < -1 || horizontal > 1
                || !Float.isFinite(payload.yRot())) return;
        if (!DashPayload.canDash(player.getMainHandItem(), vertical, horizontal,
                player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU), player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                player.hasEffect(ModEffects.CHARGING_CRASH_GU))) return;
        if (ApertureNourishService.isCultivating(player)) return;

        EpicFightIntegration.dash(player, vertical, payload.yRot());
    }

    private static void openRefinement(OpenRefinementPayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        if (player == null || !ApertureService.isAwakened(player)) return;

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new RefinementMenu(id, inventory),
                Component.translatable(REFINEMENT_TITLE)));
    }

    private static void setSecondaryPath(SetSecondaryPathPayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        int aperture = payload.aperture();
        if (player == null || !hasAperture(player, aperture)) return;

        ApertureService.setSecondaryPath(player, aperture, payload.path());
    }

    private static void openStorage(OpenApertureStoragePayload payload, IPayloadContext context) {
        ServerPlayer player = outsideTyh(context);
        int aperture = payload.aperture();
        if (player == null || !hasAperture(player, aperture)) return;

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ApertureStorageMenu(id, inventory, aperture, 0),
                Component.translatable(STORAGE_TITLE)));
    }
}
