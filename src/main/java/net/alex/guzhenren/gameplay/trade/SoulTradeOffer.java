package net.alex.guzhenren.gameplay.trade;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.jetbrains.annotations.NotNull;

/**
 * One trade a soul trader offers: the costs taken from the player's bag, and the result handed back.
 *
 * <p>Costs are vanilla {@link ItemCost}s because their count is not capped at a stack -- a price of three
 * stacks of dirt is one cost of 192 -- and because they already travel over the network. Only the 36 main
 * inventory slots ({@link Inventory#items}) pay and receive; armor and the offhand never do.
 *
 * <p>{@link #settle(NonNullList, List)} is all or nothing: it pays and stows on copies and writes back only when
 * both succeed, and then only the slots whose contents changed, so untouched stacks keep their identity. The trade is also refused unless everything that will later fall back into the
 * bag still fits afterward -- the stack on the cursor and the 2x2 crafting grid, passed as {@code returning}.
 * Inside the Treasure Yellow Heaven [宝黄天] such a return has nowhere else to go.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTradeMenu
 * @since 1.0.0
 */

public record SoulTradeOffer(List<ItemCost> costs, ItemStack result) {

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulTradeOffer> STREAM_CODEC = StreamCodec.composite(
            ItemCost.STREAM_CODEC.apply(ByteBufCodecs.list()), SoulTradeOffer::costs,
            ItemStack.STREAM_CODEC, SoulTradeOffer::result,
            SoulTradeOffer::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, List<SoulTradeOffer>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.list());

    public SoulTradeOffer {
        costs = List.copyOf(costs);
        result = result.copy();
    }

    public static SoulTradeOffer of(ItemStack result, ItemCost... costs) {
        return new SoulTradeOffer(List.of(costs), result);
    }

    @Override
    public @NotNull ItemStack result() { return result.copy(); }

    public static int held(List<ItemStack> items, ItemCost cost) {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && cost.test(stack)) count += stack.getCount();
        }
        return count;
    }

    public boolean affordable(List<ItemStack> items) {
        for (ItemCost cost : costs) {
            if (held(items, cost) < cost.count()) return false;
        }
        return true;
    }

    public Outcome settle(Inventory inventory, List<ItemStack> returning) {
        Outcome outcome = settle(inventory.items, returning);
        if (outcome == Outcome.DONE) inventory.setChanged();
        return outcome;
    }

    public Outcome settle(NonNullList<ItemStack> items, List<ItemStack> returning) {
        List<ItemStack> after = copies(items);
        for (ItemCost cost : costs) {
            if (!take(after, cost)) return Outcome.SHORT;
        }
        if (overflows(after, result.copy())) return Outcome.NO_ROOM;

        List<ItemStack> later = copies(after);
        for (ItemStack back : returning) {
            if (overflows(later, back.copy())) return Outcome.NO_ROOM;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = after.get(i);
            if (!ItemStack.matches(items.get(i), stack)) items.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        return Outcome.DONE;
    }

    private static List<ItemStack> copies(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack stack : items) copies.add(stack.copy());
        return copies;
    }

    private static boolean take(List<ItemStack> items, ItemCost cost) {
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

    private static boolean overflows(List<ItemStack> items, ItemStack stack) {
        for (ItemStack held : items) {
            if (stack.isEmpty()) return false;
            if (held.isEmpty() || !ItemStacks.sameItemSameData(held, stack)) continue;

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
