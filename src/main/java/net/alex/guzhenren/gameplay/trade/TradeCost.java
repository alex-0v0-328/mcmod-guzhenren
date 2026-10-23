package net.alex.guzhenren.gameplay.trade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * One price in a soul trade: an item, any data, and a count that is not capped at a stack -- a price of three
 * stacks of dirt is one cost of 192.
 *
 * <p>Travels as the item's registry id and the count, so it reads the same on every loader.
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

    public ItemStack itemStack() { return new ItemStack(item); }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
        buf.writeVarInt(count);
    }

    public static TradeCost decode(@NotNull FriendlyByteBuf buf) {
        return new TradeCost(BuiltInRegistries.ITEM.get(buf.readResourceLocation()), buf.readVarInt());
    }
}
