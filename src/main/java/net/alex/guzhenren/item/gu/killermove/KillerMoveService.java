package net.alex.guzhenren.item.gu.killermove;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Killer moves [杀招] on the server: a refined main Gu in the main hand driven in place, backed by a refined partner
 * from another hotbar slot ({@link KillerMove}), for one stronger use. The client's radial menu [扇形选择器] sends
 * the partner's slot, or {@link #NONE} for a plain use (a tap, or a hold with no partner listed); {@link #drive}
 * checks it all again.
 *
 * <p>A plain use is the main Gu's own in-place use ({@code tryUseInPlace}), billed and refused as that is. A killer
 * move first refuses with the essence message when the aperture cannot pay both uses together, so the partner is
 * never billed for a use the main cannot finish; any other mismatch (not a hotbar slot, the selected one, a wild or
 * wrong partner, a partner on the vanilla cooldown) lands nothing, silently.
 *
 * <p>⚠ The partner is billed from inside the main Gu's payout: {@link #drive} sets a per-player driving context
 * around the in-place use, and {@link #getDamageFactor}, asked by the main Gu's blade, bills the partner
 * ({@code TendedGuItem#billAsPartner}: its use essence and hunger, and on an empty bar its death after the blade)
 * and answers the row's factor. The first ask spends the context, so the partner pays once and any later ask in the
 * same use answers 1; outside a drive, or for another Gu, it always answers 1. A use the main Gu refuses (cooldown,
 * essence) never reaches the ask, so the partner stays unbilled.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillerMove
 * @see net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem
 * @since 1.0.0
 */

public final class KillerMoveService {

    /** The slot a plain use sends: no partner, the main Gu alone. */
    public static final int NONE = -1;
    private static final String FAILED_ESSENCE = "guzhenren.item.failed.essence";
    private static @Nullable Driving driving;

    private record Driving(ServerPlayer player, TendedGuItem main, TendedGuItem partner, ItemStack partnerStack,
                           KillerMove move) {}

    private KillerMoveService() {}

    public static void drive(@NotNull ServerPlayer player, int slot) {
        ItemStack stack = player.getMainHandItem();
        TendedGuItem main = KillerMove.asRefinedMain(stack);
        if (main == null) return;
        if (slot == NONE) {
            main.tryUseInPlace(player, stack);
            return;
        }
        if (!Inventory.isHotbarSlot(slot) || slot == player.getInventory().selected) return;

        ItemStack partnerStack = player.getInventory().getItem(slot);
        KillerMove move = KillerMove.find(main, partnerStack);
        if (move == null || player.getCooldowns().isOnCooldown(partnerStack.getItem())) return;

        TendedGuItem partner = (TendedGuItem) partnerStack.getItem();
        if (ApertureEssenceService.getSpendable(player) < main.getUseEssence() + partner.getUseEssence()) {
            ActionBar.refuse(player, FAILED_ESSENCE);
            return;
        }
        driving = new Driving(player, main, partner, partnerStack, move);
        try {
            main.tryUseInPlace(player, stack);
        } finally {
            driving = null;
        }
    }

    public static float getDamageFactor(@NotNull ServerPlayer player, @NotNull MortalGuItem gu) {
        Driving current = driving;
        if (current == null || current.player() != player || current.main() != gu) return 1.0F;

        driving = null;
        return current.partner().billAsPartner(player, current.partnerStack()) ? current.move().getDamageFactor()
                : 1.0F;
    }
}
