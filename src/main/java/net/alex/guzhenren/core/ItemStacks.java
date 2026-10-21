package net.alex.guzhenren.core;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The item-stack operations whose vanilla API differs between game versions, behind one name each.
 *
 * <p>{@link #OPTIONAL_CODEC} saves a stack that may be empty, so a list keeps an empty slot in place;
 * {@link #sameItemSameData} compares item and attached data, ignoring count.
 *
 * <p>⚠ A version seam: the NeoForge 1.21.1 mainline has its own copy with the same names (data
 * components instead of item NBT). Callers use these names only, so they stay identical between the two.
 *
 * <p>⚠ 1.20.1 has no optional stack codec: an empty stack is written as an empty compound tag and read
 * back as {@link ItemStack#EMPTY}; every other stack goes through {@link ItemStack#CODEC}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ItemStacks {

    private ItemStacks() {}

    private static final Codec<ItemStack> EMPTY_STACK = CompoundTag.CODEC.flatXmap(
            tag -> tag.isEmpty()
                    ? DataResult.success(ItemStack.EMPTY)
                    : DataResult.error(() -> "Not an empty stack marker: " + tag),
            stack -> stack.isEmpty()
                    ? DataResult.success(new CompoundTag())
                    : DataResult.error(() -> "Not an empty stack: " + stack));

    public static final Codec<ItemStack> OPTIONAL_CODEC = Codec.either(EMPTY_STACK, ItemStack.CODEC).xmap(
            either -> either.map(empty -> empty, stack -> stack),
            stack -> stack.isEmpty() ? Either.left(stack) : Either.right(stack));

    public static boolean sameItemSameData(@NotNull ItemStack a, @NotNull ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }
}
