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
 * 自动填充：选中蛊方后，把背包能凑出的材料拉进蛊方指定的格子。
 * <ul>
 *   <li>格内已有该材料不接受之物时不动该格；一格只叠同物品同组件的堆，至多到堆叠上限。</li>
 *   <li>按背包槽位顺序遍历，每种材料只取其组仍缺的数量，依序填该组格子；他人的本命蛊从不拉取。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementMenu
 * @since 1.0.0
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
