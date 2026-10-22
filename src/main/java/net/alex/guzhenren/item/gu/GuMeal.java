package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One meal a tended Gu takes from a food stack: {@code items} taken off the stack, {@code gained} points
 * (hunger or health) and {@code eaten} items actually used up. {@link #portion} never feeds past the need,
 * and rounds the items eaten UP so a part-used item is paid for; {@link #returnEmptyContainers} hands back
 * the bucket or bottle a used-up food leaves.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuClock
 * @see TendedGuItem
 * @since 1.0.0
 */

record GuMeal(int items, int gained, int eaten) {

    static GuMeal portion(int count, int need, int per, int units) {
        int items = Math.min(count, need * per / units);
        int gained = items * units / per;
        return new GuMeal(items, gained, gained <= 0 ? 0 : (gained * per + units - 1) / units);
    }

    static void returnEmptyContainers(ServerPlayer player, ItemStack food, int eaten) {
        if (!food.hasCraftingRemainingItem()) return;

        ItemStack empties = food.getCraftingRemainingItem().copyWithCount(eaten);
        if (!player.getInventory().add(empties)) player.drop(empties, false);
    }
}
