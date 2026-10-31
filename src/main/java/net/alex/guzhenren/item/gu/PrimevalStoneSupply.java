package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The one supply line for primeval stones [元石]: every automatic draw on carried stones goes through here
 * -- the top-up line (refill below {@link #REFILL_BELOW_PERCENT}%, stop at {@link #REFILL_UP_TO_PERCENT}%), every
 * essence payment a Gu makes ({@link #pay}), nourishing's purse, and the refinement menu's supply slot.
 * {@link #takeFrom} is the single way a stone leaves a stack, whether the stack is stones or a Primeval Elder Gu
 * [元老蛊] vault.
 *
 * <p>⚠ A Gu pays only through {@link #pay}: the top-up line first, then the cost, the pool first and stones for any
 * rest. A channel or refining step is sized from the pool, so it tops up first ({@link #topUpFor} its floor) and then
 * pays the step. ⚠ The sources run in one order, from any bag slot, offhand included: loose stones, then each Elder
 * Gu's stock. ⚠ Stones count only for a player with an aperture to burn them through ({@link #canAfford}): the
 * essence gate stands in for {@code isAwakened}, and without the check a mortal could pay a Gu in stones.
 *
 * <p>☠ A second copy of the top-up line drifts, and the pool silently clamps whatever a caller pours past
 * the cap. ☠ {@link #trySpend}: a lump bigger than the pool burns stones straight, never through the cap --
 * the pool pays first, the stones cover the rest, and the change goes back into the pool.
 *
 * @author Alex
 * @version 1.0.0
 * @see PrimevalStoneItem
 * @see PrimevalElderGuItem
 * @since 1.0.0
 */

public final class PrimevalStoneSupply {

    private PrimevalStoneSupply() {}

    public static final int REFILL_BELOW_PERCENT = 50;
    public static final int REFILL_UP_TO_PERCENT = 80;

    public static long getEssencePerStone() {
        return ModItems.PRIMEVAL_STONE.get() instanceof PrimevalStoneItem stone ? stone.getEssence() : 0L;
    }

    public static int takeFrom(ItemStack source, int wanted) {
        if (wanted <= 0) return 0;
        if (source.getItem() instanceof PrimevalStoneItem) {
            int taken = Math.min(wanted, source.getCount());
            source.shrink(taken);
            return taken;
        }
        return source.getItem() instanceof PrimevalElderGuItem vault ? vault.drawStones(source, wanted) : 0;
    }

    //region the stone top-up [元石补给] -- one line, so two callers cannot drift apart
    public static boolean isTopUpNeeded(Player player) {
        long max = ApertureEssenceService.getMaxEssence(player);
        return max > 0L && ApertureEssenceService.getCurrentEssence(player) * 100L < max * REFILL_BELOW_PERCENT;
    }

    public static long topUpDeficit(Player player) {
        return ApertureEssenceService.getMaxEssence(player) * REFILL_UP_TO_PERCENT / 100L
                - ApertureEssenceService.getCurrentEssence(player);
    }

    public static void topUp(ServerPlayer player) {
        if (isTopUpNeeded(player)) pourInto(player, topUpDeficit(player));
    }

    /** The top-up line, then, still short of {@code need} and the pool able to hold it, up to {@code need}. */
    public static void topUpFor(ServerPlayer player, long need) {
        topUp(player);
        long missing = need - ApertureEssenceService.getSpendable(player);
        if (missing > 0L && ApertureEssenceService.getMaxEssence(player) >= need) pourInto(player, missing);
    }

    /** What a Gu pays essence through: false, paying nothing, only when the pool and the stones together cannot. */
    public static boolean pay(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        topUp(player);
        return trySpend(player, cost);
    }
    //endregion

    //region sourcing stones for something else -- the walk lives here, not in a service
    public static void pourInto(ServerPlayer player, long wanted) {
        long each = getEssencePerStone();
        if (wanted <= 0L || each <= 0L) return;
        int taken = draw(player, (int) Math.min(Integer.MAX_VALUE, (wanted + each - 1) / each));
        if (taken > 0) ApertureEssenceService.add(player, taken * each);
    }

    private static int draw(ServerPlayer player, int wanted) {
        int taken = drawFrom(player, wanted, true);
        return taken + drawFrom(player, wanted - taken, false);
    }

    private static int drawFrom(ServerPlayer player, int wanted, boolean loose) {
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && taken < wanted; i++) {
            ItemStack stack = inventory.getItem(i);
            if (loose ? stack.getItem() instanceof PrimevalStoneItem : stack.getItem() instanceof PrimevalElderGuItem) {
                taken += takeFrom(stack, wanted - taken);
            }
        }
        return taken;
    }
    //endregion

    //region ☠ a lump bigger than the pool -- these stones burn straight, never through the cap
    public static long getWorthOnHand(Player player) {
        long stones = 0L;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PrimevalStoneItem) stones += stack.getCount();
            if (stack.getItem() instanceof PrimevalElderGuItem) stones += PrimevalElderGuItem.getStored(stack);
        }
        return stones * getEssencePerStone();
    }

    public static boolean canAfford(Player player, long cost) {
        long spendable = ApertureEssenceService.getSpendable(player);
        if (spendable >= cost) return true;
        return ApertureEssenceService.getMaxEssence(player) > 0L && spendable + getWorthOnHand(player) >= cost;
    }

    public static boolean trySpend(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        long each = getEssencePerStone();
        if (each <= 0L || !canAfford(player, cost)) return false;

        long fromPool = Math.min(ApertureEssenceService.getSpendable(player), cost);
        ApertureEssenceService.consume(player, fromPool);

        long owed = cost - fromPool;
        if (owed <= 0L) return true;
        long drawn = draw(player, (int) Math.min(Integer.MAX_VALUE, (owed + each - 1) / each)) * each;
        if (drawn > owed) ApertureEssenceService.add(player, drawn - owed);
        return true;
    }
    //endregion
}
