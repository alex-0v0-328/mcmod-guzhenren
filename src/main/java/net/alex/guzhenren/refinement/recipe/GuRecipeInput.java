package net.alex.guzhenren.refinement.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * 炼蛊网格的逐格快照，供 {@link GuRecipe} 匹配。
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public record GuRecipeInput(List<ItemStack> slots) {

    public static GuRecipeInput of(Container container) {
        List<ItemStack> slots = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) slots.add(container.getItem(i));
        return new GuRecipeInput(List.copyOf(slots));
    }

    public ItemStack getItem(int index) { return slots.get(index); }

    public int size() { return slots.size(); }
}
