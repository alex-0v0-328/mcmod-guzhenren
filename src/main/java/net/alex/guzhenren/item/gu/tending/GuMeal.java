package net.alex.guzhenren.item.gu.tending;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]从一堆食物吃的一顿：取下 {@code items}，得饱食[Hunger]或生命 {@code gained}。
 * {@link #createPortion} 不超需求，实耗 {@code eaten} 向上取整，部分用掉也付；{@link #eat} 取下（创造免费）并还桶瓶。
 *
 * @author Alex
 * @version 1.0.0
 * @see GuHunger
 * @see TendedGuItem
 * @since 1.0.0
 */

record GuMeal(int items, int gained, int eaten) {

    static GuMeal createPortion(int count, int need, int per, int units) {
        int items = Math.min(count, need * per / units);
        int gained = items * units / per;
        return new GuMeal(items, gained, gained <= 0 ? 0 : (gained * per + units - 1) / units);
    }

    /** 吃掉 {@code food} 中补足 {@code need} 的份，返回所得点数（无则 0）。 */
    static int eat(ServerPlayer player, ItemStack food, int need, int per, int units) {
        GuMeal meal = createPortion(food.getCount(), need, per, units);
        if (meal.gained() <= 0) return 0;

        if (!player.getAbilities().instabuild) {
            returnEmptyContainers(player, food, meal.eaten());
            food.shrink(meal.eaten());
        }
        return meal.gained();
    }

    private static void returnEmptyContainers(ServerPlayer player, ItemStack food, int eaten) {
        if (!food.hasCraftingRemainingItem()) return;

        ItemStack empties = food.getCraftingRemainingItem().copyWithCount(eaten);
        if (!player.getInventory().add(empties)) player.drop(empties, false);
    }
}
