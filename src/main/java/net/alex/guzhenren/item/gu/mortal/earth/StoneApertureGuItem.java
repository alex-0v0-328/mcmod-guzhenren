package net.alex.guzhenren.item.gu.mortal.earth;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.item.gu.ConsumedGuItem;
import net.alex.guzhenren.item.gu.GuSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Stone Aperture Gu [石窍蛊]: its target hits this rank's peak and petrifies -- never nourished,
 * never struck again.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.ConsumedGuItem}, so it is tended, never feeds and
 * is taken by its own use. Three rungs register against this one class. {@link #stoneTarget} is the
 * pure seam the unit tests pin: the target is the PRIMARY aperture whenever it is NORMAL; only a
 * dead or stoned primary passes the Gu on, and only to a NORMAL second aperture; nobody usable
 * answers {@code NO_TARGET}. {@link #targetOf} maps that seam onto real list positions: a lone
 * second aperture lives at position 0, and a missing slot counts as DEAD. A rank mismatch on the
 * primary does NOT pass it on -- the fall-through is for a lost aperture, not for a wrong rank. The
 * gate refuses no usable target, a rank mismatch on the target, and a target already at the peak
 * (that use would buy only the lock). The payout delegates to {@link ApertureNourishService#petrify}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.ConsumedGuItem
 * @since 1.0.0
 */

public class StoneApertureGuItem extends ConsumedGuItem {

    public static final int NO_TARGET = -1;
    private static final String FAILED_UNAVAILABLE = "guzhenren.item.failed.aperture_unavailable";

    public StoneApertureGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    public static int stoneTarget(@NotNull ApertureStatus primary, @NotNull ApertureStatus second) {
        if (primary == ApertureStatus.NORMAL) return ApertureData.PRIMARY;
        if (second == ApertureStatus.NORMAL) return ApertureData.SECOND;
        return NO_TARGET;
    }

    private static int targetOf(Player player) {
        ApertureData data = ApertureService.get(player);
        int primary = data.firstIndex();
        int second = data.secondIndex();
        int target = stoneTarget(
                primary < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, primary),
                second < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, second));
        if (target == NO_TARGET) return NO_TARGET;
        return target == ApertureData.SECOND ? Math.max(second, 0) : Math.max(primary, 0);
    }

    @Override
    protected @Nullable Refusal payoutGate(Player player, ItemStack stack) {
        int target = targetOf(player);
        if (target == NO_TARGET) return new Refusal(FAILED_UNAVAILABLE);
        return stageUpGate(ApertureService.getAperture(player, target));
    }

    @Override
    protected void payout(ServerPlayer player, ItemStack stack) {
        int target = targetOf(player);
        if (target != NO_TARGET) ApertureNourishService.petrify(player, target);
    }
}
