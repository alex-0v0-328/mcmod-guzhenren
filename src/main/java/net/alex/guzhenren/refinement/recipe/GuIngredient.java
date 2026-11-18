package net.alex.guzhenren.refinement.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * {@link GuRecipe} 的单条材料需求记录，封装可接受的 {@link Ingredient} 与精确所需数量；属于模组自有数据记录而非原版类
 * 型，由 {@link RefinementRecipe} 按扁平 JSON 字段读写。
 */

public record GuIngredient(Ingredient ingredient, int count) {

    public GuIngredient {
        if (count <= 0) throw new IllegalArgumentException("count must be positive");
    }

    public static GuIngredient of(ItemLike item, int count) { return new GuIngredient(Ingredient.of(item), count); }

    public boolean test(ItemStack stack) { return ingredient.test(stack); }
}
