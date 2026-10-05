package net.alex.guzhenren.gameplay.refinement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * What the refinement [炼蛊] grid is holding, in the shape the recipe manager wants to see it.
 *
 * <p>Implements {@link net.minecraft.world.item.crafting.RecipeInput} as an immutable snapshot of the
 * grid's slots. Built from a {@link net.minecraft.world.Container} via {@code of()}, which copies the
 * slot LIST but hands out the live stacks: a recipe match reads them and must never change one.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public record GuRecipeInput(List<ItemStack> slots) implements RecipeInput {

    public static GuRecipeInput of(Container container) {
        List<ItemStack> slots = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) slots.add(container.getItem(i));
        return new GuRecipeInput(List.copyOf(slots));
    }

    @Override
    public @NotNull ItemStack getItem(int index) { return slots.get(index); }

    @Override
    public int size() { return slots.size(); }
}
