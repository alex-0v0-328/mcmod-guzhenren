package net.alex.guzhenren.gameplay.trade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * 交易魂交易的一项价格：物品（不论数据）加不受单组上限的数量，3 组泥土即一项 192；按物品注册 ID 与数量传输。
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTradeOffer
 * @since 1.0.0
 */

public record TradeCost(Item item, int count) {

    public TradeCost {
        if (count <= 0) throw new IllegalArgumentException("count must be positive");
    }

    public static TradeCost of(ItemLike item, int count) { return new TradeCost(item.asItem(), count); }

    public boolean test(@NotNull ItemStack stack) { return stack.is(item); }

    public ItemStack createItemStack() { return new ItemStack(item); }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
        buf.writeVarInt(count);
    }

    public static TradeCost decode(@NotNull FriendlyByteBuf buf) {
        return new TradeCost(BuiltInRegistries.ITEM.get(buf.readResourceLocation()), buf.readVarInt());
    }
}
