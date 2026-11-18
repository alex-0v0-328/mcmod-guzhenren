package net.alex.guzhenren.gameplay.trade;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 交易魂的单项报价记录：封装待扣除的一组 {@link TradeCost} 代价与交回的 {@code result} 物品。
 * <ul>
 *   <li>单笔代价数量不受物品最大堆叠上限限制；结算仅读取并修改 36 个主背包格（{@link Inventory#items}），护甲槽与
 *       副手不参与扣费或收货；经 {@link #encode} 与 {@link #decode} 随开窗数据包同步至客户端。</li>
 *   <li>{@link #settle(NonNullList, List)} 采用全有或全无原子结算：先在列表副本上扣费与试放，全部通过后才写回原
 *       背包，且仅覆盖发生变化的槽位以保留未动物品堆的原实例。</li>
 *   <li>⚠ 结算额外校验关闭界面后将落回背包的 {@code returning} 物品（光标物品与 2×2 合成格）：由于
 *       宝黄天[Treasure Yellow Heaven]内不允许凭空丢弃或无处安放，一旦回退物品溢出同样返回
 *       {@link Outcome#NO_ROOM} 拒绝交易。</li>
 * </ul>
 */

public record SoulTradeOffer(List<TradeCost> costs, ItemStack result) {

    public SoulTradeOffer {
        costs = List.copyOf(costs);
        result = result.copy();
    }

    public static SoulTradeOffer of(ItemStack result, TradeCost... costs) {
        return new SoulTradeOffer(List.of(costs), result);
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(costs.size());
        for (TradeCost cost : costs) cost.encode(buf);
        ItemStacks.writeStack(buf, result);
    }

    public static SoulTradeOffer decode(@NotNull FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TradeCost> costs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) costs.add(TradeCost.decode(buf));
        return new SoulTradeOffer(costs, ItemStacks.readStack(buf));
    }

    public static void encodeList(@NotNull FriendlyByteBuf buf, List<SoulTradeOffer> offers) {
        buf.writeVarInt(offers.size());
        for (SoulTradeOffer offer : offers) offer.encode(buf);
    }

    public static List<SoulTradeOffer> decodeList(@NotNull FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SoulTradeOffer> offers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) offers.add(decode(buf));
        return offers;
    }

    @Override
    public @NotNull ItemStack result() { return result.copy(); }

    public static int countHeld(List<ItemStack> items, TradeCost cost) {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && cost.test(stack)) count += stack.getCount();
        }
        return count;
    }

    public boolean canAfford(List<ItemStack> items) {
        for (TradeCost cost : costs) {
            if (countHeld(items, cost) < cost.count()) return false;
        }
        return true;
    }

    public Outcome settle(Inventory inventory, List<ItemStack> returning) {
        Outcome outcome = settle(inventory.items, returning);
        if (outcome == Outcome.DONE) inventory.setChanged();
        return outcome;
    }

    public Outcome settle(NonNullList<ItemStack> items, List<ItemStack> returning) {
        List<ItemStack> after = copyAll(items);
        for (TradeCost cost : costs) {
            if (!tryTake(after, cost)) return Outcome.SHORT;
        }
        if (isOverflowing(after, result.copy())) return Outcome.NO_ROOM;

        List<ItemStack> later = copyAll(after);
        for (ItemStack back : returning) {
            if (isOverflowing(later, back.copy())) return Outcome.NO_ROOM;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = after.get(i);
            if (!ItemStack.matches(items.get(i), stack)) items.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        return Outcome.DONE;
    }

    private static List<ItemStack> copyAll(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack stack : items) copies.add(stack.copy());
        return copies;
    }

    private static boolean tryTake(List<ItemStack> items, TradeCost cost) {
        int wanted = cost.count();
        for (ItemStack stack : items) {
            if (wanted <= 0) break;
            if (stack.isEmpty() || !cost.test(stack)) continue;

            int taken = Math.min(wanted, stack.getCount());
            stack.shrink(taken);
            wanted -= taken;
        }
        return wanted <= 0;
    }

    private static boolean isOverflowing(List<ItemStack> items, ItemStack stack) {
        for (ItemStack held : items) {
            if (stack.isEmpty()) return false;
            if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, stack)) continue;

            int moved = Math.min(held.getMaxStackSize() - held.getCount(), stack.getCount());
            if (moved <= 0) continue;

            held.grow(moved);
            stack.shrink(moved);
        }
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) items.set(i, stack.split(stack.getMaxStackSize()));
        }
        return !stack.isEmpty();
    }

    public enum Outcome {

        DONE,
        SHORT,
        NO_ROOM
    }
}
