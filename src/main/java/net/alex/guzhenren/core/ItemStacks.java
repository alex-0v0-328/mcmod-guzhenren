package net.alex.guzhenren.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 物品堆叠（含空）的网络读写；⚠ 只走 {@link RegistryFriendlyByteBuf}（网络层、菜单层给的都是），普通
 * {@link FriendlyByteBuf} 抛 {@link IllegalArgumentException}。
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
