package net.alex.guzhenren.item.armor;

import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 学堂制服[School Uniform]工作台换色特殊配方：1 件可染色制服 + 1 个任意原版染料，直接切换为该染料对应的 16 色古风
 * 色（不混色），保留耐久、附魔与改名等原有组件；若目标色与当前颜色相同则拒绝匹配，避免误消耗染料。
 */

public final class SchoolUniformDyeRecipe extends CustomRecipe {

    public SchoolUniformDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return findPair(input) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        Pair pair = findPair(input);
        if (pair == null) return ItemStack.EMPTY;
        ItemStack result = pair.uniform().copyWithCount(1);
        UniformColor.setColor(result, pair.color());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.SCHOOL_UNIFORM_DYE_SERIALIZER.get();
    }

    private static Pair findPair(CraftingInput input) {
        ItemStack uniform = ItemStack.EMPTY;
        DyeColor color = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof SchoolUniformItem item && item.isDyeable()) {
                if (!uniform.isEmpty()) return null;
                uniform = stack;
            } else if (stack.getItem() instanceof DyeItem dye) {
                if (color != null) return null;
                color = dye.getDyeColor();
            } else {
                return null;
            }
        }
        if (uniform.isEmpty() || color == null) return null;
        if (UniformColor.getColor(uniform) == color) return null;
        return new Pair(uniform, color);
    }

    private record Pair(ItemStack uniform, DyeColor color) {}
}
