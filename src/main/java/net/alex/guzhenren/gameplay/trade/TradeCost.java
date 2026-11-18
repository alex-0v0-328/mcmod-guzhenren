package net.alex.guzhenren.gameplay.trade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * 交易魂报价的单项材料代价：由目标物品类型（不校验组件数据）与正整数需求数量组成。
 * <ul>
 *   <li>需求数量不受单组最大堆叠上限限制（例如 3 组泥土直接记为 {@code 192}）；通过物品注册 ID 与 {@code VarInt} 数
 *       量进行网络编解码（供 {@link SoulTradeOffer} 传输）。</li>
 * </ul>
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
