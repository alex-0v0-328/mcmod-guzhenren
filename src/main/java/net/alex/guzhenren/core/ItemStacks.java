package net.alex.guzhenren.core;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The item-stack operations whose vanilla API differs between game versions, behind one name each.
 *
 * <p>{@link #OPTIONAL_CODEC} saves a stack that may be empty, so a list keeps an empty slot in place;
 * {@link #sameItemSameData} compares item and attached data, ignoring count.
 *
 * <p>⚠ A version seam: the Forge 1.20.1 port has its own copy with the same names (item NBT instead of
 * data components). Callers use these names only, so they stay identical between the two.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ItemStacks {

    private ItemStacks() {}

    public static final Codec<ItemStack> OPTIONAL_CODEC = ItemStack.OPTIONAL_CODEC;

    public static boolean sameItemSameData(@NotNull ItemStack a, @NotNull ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
