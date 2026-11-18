package net.alex.guzhenren.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 支持空堆叠的 {@link ItemStack} 网络字节流读写工具，封装 {@link ItemStack#OPTIONAL_STREAM_CODEC}。
 * <ul>
 *   <li>⚠ 传入的缓冲区在运行期必须为携带注册表上下文的 {@link RegistryFriendlyByteBuf}（网络负载与容器菜单同步底层均提
 *       供该类型），传入普通 {@link FriendlyByteBuf} 会直接抛出 {@link IllegalArgumentException}。</li>
 * </ul>
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
