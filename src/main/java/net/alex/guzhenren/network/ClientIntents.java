package net.alex.guzhenren.network;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.ChargedPunch;
import net.alex.guzhenren.network.payload.ChargeAction;
import net.alex.guzhenren.network.payload.ElderAction;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.NourishAction;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the server does with each client intent: the two containers, the secondary path, the three
 * cultivation actions, the dash, the second-stage flight [二段飞行] toggle, the plunge [坠击], the Amplify Effect
 * Gu's [功倍蛊] radial-menu pick, the Primeval Elder Gu's [元老蛊] tap and slider pick, the Strength Qi Gu's [力气蛊]
 * ranged punch, the Store Strength Gu's [蓄力蛊] charge (a refused start answers with the drop notice,
 * {@code StoreStrengthNotices}), and a moon blade Gu's [月刃]
 * use (the killing move [杀招] picked, or none for a plain use; a cast that lands answers the caster with the Killing
 * Move Tooltip's [杀招提示] notice). Every intent but
 * that toggle refuses inside the Treasure Yellow Heaven ({@link #isOutsideTyh}), where flight and its second stage
 * are what remains; the plunge's own rule refuses it there ({@code PlungeService#canStart}), which the client asks
 * too.
 *
 * <p>Every client intent in this mod is a B-panel button, a movement input or a held-input gesture that player-data
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

    /** The main-hand Primeval Elder Gu's tap (its use, the deposit) or its slider pick of {@code stacks} stacks. */
    public static void primevalElder(@NotNull ServerPlayer player, @NotNull ElderAction action, int stacks) {
        ItemStack stack = player.getMainHandItem();
        if (!isOutsideTyh(player) || player.isSpectator()) return;
        if (!(stack.getItem() instanceof PrimevalElderGuItem elder)) return;
        if (!elder.isRefined(stack)) return;
        switch (action) {
            case DEPOSIT -> elder.tryUseInPlace(player, stack);
            case WITHDRAW -> elder.tryWithdraw(player, stack, stacks);
        }
    }

    public static void killingMove(@NotNull ServerPlayer player, @NotNull String move) {
        killingMove(player, move, KillingMoveService.NO_TARGET);
    }

    /** Drives the main-hand blade Gu at the client's locked target [锁定目标], {@code NO_TARGET} for none. */
    public static void killingMove(@NotNull ServerPlayer player, @NotNull String move, int target) {
        if (!isOutsideTyh(player)) return;

        KillingMoveService.Cast cast = KillingMoveService.drive(player, move, target);
        if (cast != null) PacketDistributor.sendToPlayer(player, KillingMoveTooltipPayload.of(cast));
    }

    /** A Strength Qi Gu [力气蛊] punch thrown as a Qi Wave [气波] at the client's target. */
    public static void qiWave(@NotNull ServerPlayer player, int target) {
        if (isOutsideTyh(player)) QiWaveService.fire(player, target);
    }

    public static void storeStrength(@NotNull ServerPlayer player, @NotNull ChargeAction action, int target) {
        switch (action) {
            case START -> {
                if (isOutsideTyh(player) && StoreStrength.start(player)) {
                    EpicFightIntegration.holdCharge(player);
                } else {
                    StoreStrengthNotices.send(player, StoreStrengthChargeEvent.Change.DROPPED);
                }
            }
            case CANCEL -> {
                if (StoreStrength.isCharging(player)) EpicFightIntegration.endCharge(player);
                StoreStrength.cancel(player);
            }
            case RELEASE -> {
                boolean charging = StoreStrength.isCharging(player);
                if (isOutsideTyh(player) && ChargedPunch.release(player, target)) {
                    EpicFightIntegration.strikeCharge(player);
                } else {
                    StoreStrength.cancel(player);
                    if (charging) EpicFightIntegration.endCharge(player);
                }
            }
        }
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
