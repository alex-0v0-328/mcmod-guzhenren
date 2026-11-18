package net.alex.guzhenren.refinement;

import java.util.List;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 炼蛊配方选中后的材料自动填充工具（供 {@link RefinementMenu} 调用）：按背包槽位顺序将尚缺数量的材料依次填入对应配方格
 * 组。若目标格已放入当前配方不接受的物品则跳过该格，同一格仅堆叠同物品同组件的物品堆至堆叠上限，并始终跳过属于他人的本
 * 命蛊。
 */

final class RecipeAutofill {

    private RecipeAutofill() {}

    static void fill(GuRecipe recipe, Container input, Player player) {
        for (int n = 0; n < recipe.ingredients().size(); n++) {
            GuIngredient need = recipe.ingredients().get(n);
            List<Integer> cells = recipe.slots().get(n);
            int wanted = need.count() - countHeld(need, input, cells);

            for (int slot : cells) {
                if (wanted <= 0) break;
                if (slot < 0 || slot >= GuRecipe.INPUT_SIZE) continue;

                ItemStack held = input.getItem(slot);
                if (!held.isEmpty() && !need.test(held)) continue;

                wanted -= draw(need, input, slot, wanted, player);
            }
        }
    }

    private static int countHeld(GuIngredient need, Container input, List<Integer> cells) {
        int held = 0;
        for (int slot : cells) {
            if (slot < 0 || slot >= GuRecipe.INPUT_SIZE) continue;

            ItemStack stack = input.getItem(slot);
            if (need.test(stack)) held += stack.getCount();
        }
        return held;
    }

    private static int draw(GuIngredient need, Container input, int slot, int wanted, Player player) {
        Inventory inventory = player.getInventory();
        int left = wanted;

        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack from = inventory.getItem(i);
            if (from.isEmpty() || !need.test(from)) continue;
            if (VitalGuState.isForeignVital(from, player)) continue;

            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, from)) continue;

            int move = Math.min(Math.min(left, from.getCount()),
                    from.getMaxStackSize() - held.getCount());
            if (move <= 0) continue;

            if (held.isEmpty()) {
                input.setItem(slot, from.split(move));
            } else {
                held.grow(move);
                from.shrink(move);
                input.setChanged();
            }
            left -= move;
        }
        return wanted - left;
    }
}
