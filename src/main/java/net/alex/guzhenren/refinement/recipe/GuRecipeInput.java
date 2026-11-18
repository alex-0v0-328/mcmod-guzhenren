package net.alex.guzhenren.refinement.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * 炼蛊输入网格的只读逐格快照，用于在 {@link GuRecipe} 中执行确定性材料匹配与缺口计算。
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
