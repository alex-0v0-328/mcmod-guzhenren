package net.alex.guzhenren.network;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the server does with each client intent: the two containers, the secondary path, the three
 * cultivation actions, the dash, the second-stage flight [二段飞行] toggle, the plunge [坠击] and the Amplify Effect
 * Gu's [功倍蛊] radial-menu pick. Every intent but that toggle refuses inside the Treasure Yellow Heaven
 * ({@link #isOutsideTyh}), where flight and its second stage are what remains; the plunge's own rule refuses it
 * there ({@code PlungeService#canStart}), which the client asks too.
 *
 * <p>Every client intent in this mod is a B-panel button, a movement input or a radial-menu pick that player-data
 * sync cannot carry upstream; none carries player data, and downstream player data always travels as synced state.
 * The packet layer ({@code ModPayloads}) decodes each packet and calls the matching method here with the
 * sender; this class holds every check and every effect.
 *
 * <p>⚠ This is where a forged packet lands, so a gate that only grays out a button is not a gate: every
 * refusal has to exist here as well as on the screen.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

public final class ClientIntents {

    private ClientIntents() {}

    public enum NourishAction { START, CANCEL }

    private static final String STORAGE_TITLE = "guzhenren.menu.aperture_storage";
    private static final String REFINEMENT_TITLE = "guzhenren.menu.refinement";

    private static boolean isOutsideTyh(@NotNull ServerPlayer player) {
        return !player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    private static boolean hasAperture(@NotNull ServerPlayer player, int index) {
        return index >= 0 && index < ApertureService.get(player).count();
    }

    public static boolean canDash(int vertical, int horizontal,
                                  boolean horizontalCrash, boolean verticalCrash, boolean chargingCrash) {
        if (vertical == 0 && horizontal == 0) return false;
        return (horizontal == 0 || horizontalCrash || chargingCrash)
                && (vertical == 0 || verticalCrash || chargingCrash);
    }

    public static void nourishAperture(@NotNull ServerPlayer player, @NotNull NourishAction action, int aperture) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        switch (action) {
            case START -> ApertureNourishService.start(player, aperture);
            case CANCEL -> ApertureNourishService.cancel(player);
        }
    }

    public static void impactApertureWall(@NotNull ServerPlayer player) {
        if (isOutsideTyh(player)) ApertureNourishService.impactWall(player);
    }

    public static void dash(@NotNull ServerPlayer player, int vertical, int horizontal, float yRot) {
        if (!isOutsideTyh(player)) return;
        if (vertical < -1 || vertical > 1 || horizontal < -1 || horizontal > 1 || !Float.isFinite(yRot)) return;
        if (!canDash(vertical, horizontal,
                player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU),
                player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                player.hasEffect(ModEffects.CHARGING_CRASH_GU))) return;
        if (ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player)) return;
        EpicFightIntegration.dash(player, vertical, yRot);
    }

    public static void toggleSecondStage(@NotNull ServerPlayer player) { SecondStageService.toggle(player); }

    public static void plunge(@NotNull ServerPlayer player) { PlungeService.start(player); }

    public static void amplifyDrive(@NotNull ServerPlayer player, int slot) {
        if (isOutsideTyh(player)) AmplifyEffectGuItem.driveFromHotbar(player, slot);
    }

    public static void openRefinement(@NotNull ServerPlayer player) {
        if (!isOutsideTyh(player) || !ApertureService.isAwakened(player)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new RefinementMenu(id, inventory),
                Component.translatable(REFINEMENT_TITLE)));
    }

    public static void setSecondaryPath(@NotNull ServerPlayer player, int aperture, @Nullable GuPath path) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        ApertureService.setSecondaryPath(player, aperture, path);
    }

    public static void openApertureStorage(@NotNull ServerPlayer player, int aperture) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ApertureStorageMenu(id, inventory, aperture, 0),
                Component.translatable(STORAGE_TITLE)), buffer -> buffer.writeVarInt(aperture));
    }
}
