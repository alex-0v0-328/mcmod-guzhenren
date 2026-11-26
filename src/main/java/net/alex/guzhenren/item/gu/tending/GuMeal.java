package net.alex.guzhenren.item.gu.tending;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]从食物堆中取食的定量记录：按需求换算可取件数 {@code items}、所得饱食[Hunger]点数
 * {@code gained} 与实际消耗件数 {@code eaten}。{@link #createPortion} 确保所得不超过需求且实耗向上取整（部分用掉也按
 * 整件扣除），{@link #eat} 负责扣除食物（创造模式免费）并归还桶或瓶等容器。
 */

record GuMeal(int items, int gained, int eaten) {

    static GuMeal createPortion(int count, int need, int per, int units) {
        int items = Math.min(count, need * per / units);
        int gained = items * units / per;
        return new GuMeal(items, gained, gained <= 0 ? 0 : (gained * per + units - 1) / units);
    }

    /** 吃掉 {@code food} 中补足 {@code need} 的份，返回所得饱食点数（无则 0）。 */
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
