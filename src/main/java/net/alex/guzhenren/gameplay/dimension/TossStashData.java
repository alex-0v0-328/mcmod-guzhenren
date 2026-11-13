package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * What a player tossed in the Treasure Yellow Heaven [宝黄天] that did not fit back in the bag, held for the way
 * out: {@link TreasureYellowHeavenGuard#tick} drops every stack at the player's feet once they stand outside, by
 * any route. Saved and never synced; a death carries it, so a player who dies inside gets it at the respawn point.
 *
 * <p>{@link #with} tops up a held stack of the same item and components before it starts a new one, so the list
 * stays as short as what it holds.
 *
 * <p>⚠ The stacks are copies the record owns: {@link #with} copies what it takes, and a reader copies before it
 * hands a stack on.
 *
 * @author Alex
 * @version 1.0.0
 * @see TreasureYellowHeavenGuard
 * @since 1.0.0
 */

public record TossStashData(List<ItemStack> stacks) {

    public static final TossStashData DEFAULT = new TossStashData(List.of());

    public static final Codec<TossStashData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(TossStashData::stacks)
    ).apply(instance, TossStashData::new));

    public TossStashData {
        stacks = List.copyOf(stacks);
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public TossStashData with(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return this;

        ItemStack rest = stack.copy();
        List<ItemStack> held = new ArrayList<>(stacks.size() + 1);
        for (ItemStack kept : stacks) {
            ItemStack copy = kept.copy();
            if (!rest.isEmpty() && ItemStack.isSameItemSameComponents(copy, rest)) {
                int moved = Math.min(rest.getCount(), copy.getMaxStackSize() - copy.getCount());
                copy.grow(moved);
                rest.shrink(moved);
            }
            held.add(copy);
        }
        if (!rest.isEmpty()) held.add(rest);
        return new TossStashData(held);
    }
}
