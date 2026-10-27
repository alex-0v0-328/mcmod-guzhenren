package net.alex.guzhenren.refinement;

import java.util.List;
import net.alex.guzhenren.core.ItemStacks;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Autofill [自动填充]: picking a Gu Recipe [蛊方] pulls what the bag can cover into the cells the recipe names.
 *
 * <p>A cell already holding something the ingredient refuses is left alone, and a cell only ever stacks the same
 * item with the same components, up to its stack size. The bag is walked in slot order and each ingredient takes
 * only what its group still lacks, filling the group's cells in order; another player's Vital Gu [本命蛊] is
 * never pulled.
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
            if (GuItem.isForeignVital(from, player)) continue;

            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !ItemStacks.isSameItemSameData(held, from)) continue;

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
