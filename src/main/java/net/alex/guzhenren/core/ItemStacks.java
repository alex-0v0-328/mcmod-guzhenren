package net.alex.guzhenren.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The item-stack operations whose vanilla API differs between game versions, behind one name each.
 *
 * <p>{@link #OPTIONAL_CODEC} saves a stack that may be empty, so a list keeps an empty slot in place;
 * {@link #sameItemSameData} compares item and attached data, ignoring count; {@link #writeStack} and {@link
 * #readStack} carry a stack, empty or not, over the wire.
 *
 * <p>⚠ A version seam: the Forge 1.20.1 port has its own copy with the same names (item NBT instead of
 * data components). Callers use these names only, so they stay identical between the two.
 *
 * <p>⚠ On 1.21.1 a stack only travels on a {@link RegistryFriendlyByteBuf}. Every buffer the network and menu
 * layers hand out is one; a plain {@link FriendlyByteBuf} is refused with an {@link IllegalArgumentException}.
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

    public static void writeStack(@NotNull FriendlyByteBuf buf, @NotNull ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registries(buf), stack);
    }

    public static @NotNull ItemStack readStack(@NotNull FriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(registries(buf));
    }

    private static RegistryFriendlyByteBuf registries(FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf registries) return registries;
        throw new IllegalArgumentException("an item stack needs a RegistryFriendlyByteBuf, got " + buf.getClass());
    }
}
