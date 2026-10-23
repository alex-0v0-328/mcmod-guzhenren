package net.alex.guzhenren.refinement;

import net.alex.guzhenren.core.ItemStacks;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Autofill [自动填充]: picking a Gu Recipe [蛊方] pulls what the bag can cover into the cells the recipe names.
 *
 * <p>A cell already holding something the ingredient refuses is left alone, and a cell only ever stacks the same
 * item with the same components, up to its stack size. The bag is walked in slot order and each cell takes only
 * what it still lacks; another player's Vital Gu [本命蛊] is never pulled.
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
            int slot = recipe.slots().get(n);
            if (slot < 0 || slot >= GuRecipe.INPUT_SIZE) continue;

            GuIngredient need = recipe.ingredients().get(n);
            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !need.test(held)) continue;

            int wanted = need.count() - held.getCount();
            if (wanted > 0) draw(need, input, slot, wanted, player);
        }
    }

    private static void draw(GuIngredient need, Container input, int slot, int wanted, Player player) {
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize() && wanted > 0; i++) {
            ItemStack from = inventory.getItem(i);
            if (from.isEmpty() || !need.test(from)) continue;
            if (GuItem.isForeignVital(from, player)) continue;

            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !ItemStacks.sameItemSameData(held, from)) continue;

            int move = Math.min(Math.min(wanted, from.getCount()),
                    from.getMaxStackSize() - held.getCount());
            if (move <= 0) continue;

            if (held.isEmpty()) {
                input.setItem(slot, from.split(move));
            } else {
                held.grow(move);
                from.shrink(move);
                input.setChanged();
            }
            wanted -= move;
        }
    }
}
