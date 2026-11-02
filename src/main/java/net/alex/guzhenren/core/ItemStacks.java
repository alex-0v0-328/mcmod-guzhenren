package net.alex.guzhenren.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Carries an item stack, empty or not, over the wire: {@link #writeStack} and {@link #readStack}.
 *
 * <p>⚠ A stack only travels on a {@link RegistryFriendlyByteBuf}. Every buffer the network and menu
 * layers hand out is one; a plain {@link FriendlyByteBuf} is refused with an {@link IllegalArgumentException}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ItemStacks {

    private ItemStacks() {}

    public static void writeStack(@NotNull FriendlyByteBuf buf, @NotNull ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(getRegistries(buf), stack);
    }

    public static @NotNull ItemStack readStack(@NotNull FriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(getRegistries(buf));
    }

    private static RegistryFriendlyByteBuf getRegistries(FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf registries) return registries;
        throw new IllegalArgumentException("an item stack needs a RegistryFriendlyByteBuf, got " + buf.getClass());
    }
}
