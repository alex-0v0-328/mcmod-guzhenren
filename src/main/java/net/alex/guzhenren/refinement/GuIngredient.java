package net.alex.guzhenren.refinement;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * One cell's need in a Gu Recipe [蛊方]: what may sit there, and exactly how many.
 *
 * <p>The recipe's own ingredient type, so matching reads the same on every loader; {@link RefinementRecipe}
 * reads and writes it as the flat JSON object {@code {"count": n, "item": …}}.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public record GuIngredient(Ingredient ingredient, int count) {

    public GuIngredient {
        if (count <= 0) throw new IllegalArgumentException("count must be positive");
    }

    public static GuIngredient of(ItemLike item, int count) { return new GuIngredient(Ingredient.of(item), count); }

    public boolean test(ItemStack stack) { return ingredient.test(stack); }
}
