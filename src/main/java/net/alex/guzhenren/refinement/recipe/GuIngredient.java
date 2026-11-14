package net.alex.guzhenren.refinement.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * 蛊方的一条材料需求：可放物品与确切数量。
 * 自有类型，非原版 {@code Ingredient}；{@link RefinementRecipe} 按扁平 JSON {@code {"count": n, "item": …}} 读写。
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
