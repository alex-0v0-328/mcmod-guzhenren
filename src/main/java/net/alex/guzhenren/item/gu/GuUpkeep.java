package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The day-rollover walk over refined tended Gu: three containers bill through here, on either clock --
 * {@link #tickInContainer} for a Gu held in an aperture, {@link #tickCarried} for every Gu in the holder's own
 * inventory; either eats from the holder's bag before and after the day's drain. Each refined Gu pays its own
 * upkeep, then its hunger clock decides whether it starved; a starved carried Gu is removed and reported through
 * {@code TendedGuItem.starve}.
 *
 * <p>⚠ The billing step runs the meal, decay, the meal again, then warn. Nothing in the code makes that order look
 * load-bearing, and swapping any two of them changes which Gu survive a day. The first meal saves a Gu that went
 * hungry while the bag was empty and found food since: a stored Gu cannot be driven, so nothing else feeds it before
 * the drain. Both meals run only on a heartbeat that crossed a day ({@code days > 0}); each mends first, so a Gu
 * whose bag food all went on a wound is still warned.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuHunger
 * @since 1.0.0
 */

public final class GuUpkeep {

    private GuUpkeep() {}

    public static boolean tickInContainer(ServerPlayer player, ItemStack stack, long days) {
        return tickOne(player, stack, days);
    }

    public static void tickCarried(ServerPlayer player, long days) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (tickOne(player, stack, days)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                TendedGuItem.starve(player, stack);
            }
        }
    }

    private static boolean tickOne(ServerPlayer player, ItemStack stack, long days) {
        if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.isRefined(stack)) return false;

        gu.payOwnUpkeep(player, stack);
        if (days > 0L) gu.feed(player, stack);
        if (gu.hunger.tickStarvation(player, stack, days)) return true;

        if (days > 0L) gu.feed(player, stack);
        if (gu.hunger.isHungry(player, stack)) gu.hunger.warn(player, stack, days);
        return false;
    }
}
