package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The day-rollover walk over refined tended Gu: three containers bill through here, on either clock --
 * {@link #tickInContainer} for a Gu held in an aperture (it may feed itself from the holder's bag),
 * {@link #tickCarried} for every Gu in the holder's own inventory (it may not). Each refined Gu pays its own
 * upkeep, then its hunger clock decides whether it starved; a starved carried Gu is removed and reported
 * through {@code TendedGuItem.starved}.
 *
 * <p>⚠ The billing step runs decay, then auto-feed, then warn. Nothing in the code makes that order look
 * load-bearing, and swapping any two of them changes which Gu survive a day.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuClock
 * @since 1.0.0
 */

public final class GuUpkeep {

    private GuUpkeep() {}

    public static boolean tickInContainer(ServerPlayer player, ItemStack stack, long days) {
        return tickOne(player, stack, days, true);
    }

    public static void tickCarried(ServerPlayer player, long days) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (tickOne(player, stack, days, false)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                TendedGuItem.starved(player, stack);
            }
        }
    }

    private static boolean tickOne(ServerPlayer player, ItemStack stack, long days, boolean autoFeeds) {
        if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.refined(stack)) return false;

        gu.payOwnUpkeep(player, stack);
        if (gu.clock.starves(player, stack, days)) return true;

        if (autoFeeds && gu.autoFeed(player, stack)) return false;
        if (gu.clock.hungry(player, stack)) gu.clock.warn(player, stack, days);
        return false;
    }
}
