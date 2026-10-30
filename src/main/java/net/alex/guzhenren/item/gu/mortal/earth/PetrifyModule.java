package net.alex.guzhenren.item.gu.mortal.earth;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Stone Aperture Gu's [石窍蛊] module: its target hits this rank's peak and petrifies -- never nourished,
 * never struck again.
 *
 * <p>Three rungs register with it. {@link #getStoneTarget} is the pure seam the unit tests pin: the target is the
 * PRIMARY aperture whenever it is NORMAL; only a dead or stoned primary passes the Gu on, and only to a NORMAL
 * second aperture; nobody usable answers {@code NO_TARGET}. {@link #getTargetOf} maps that seam onto real list
 * positions: a lone second aperture lives at position 0, and a missing slot counts as DEAD. A rank mismatch on the
 * primary does NOT pass it on -- the fall-through is for a lost aperture, not for a wrong rank. The gate refuses no
 * usable target, a rank mismatch on the target, and a target already at the peak (that use would buy only the
 * lock). The payout delegates to {@link ApertureNourishService#petrify}. {@code StateEffects} finds the Gu of a
 * petrified rank by this module.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record PetrifyModule() implements GuModule {

    public static final int NO_TARGET = -1;
    private static final String FAILED_UNAVAILABLE = "guzhenren.item.failed.aperture_unavailable";

    public static int getStoneTarget(@NotNull ApertureStatus primary, @NotNull ApertureStatus second) {
        if (primary == ApertureStatus.NORMAL) return ApertureData.PRIMARY;
        if (second == ApertureStatus.NORMAL) return ApertureData.SECOND;
        return NO_TARGET;
    }

    private static int getTargetOf(Player player) {
        ApertureData data = ApertureService.get(player);
        int primary = data.getFirstIndex();
        int second = data.getSecondIndex();
        int target = getStoneTarget(
                primary < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, primary),
                second < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, second));
        if (target == NO_TARGET) return NO_TARGET;
        return target == ApertureData.SECOND ? Math.max(second, 0) : Math.max(primary, 0);
    }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        int target = getTargetOf(player);
        if (target == NO_TARGET) return new Refusal(FAILED_UNAVAILABLE);
        return gu.checkStageUpGate(ApertureService.getAperture(player, target));
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        int target = getTargetOf(player);
        if (target != NO_TARGET) ApertureNourishService.petrify(player, target);
    }
}
