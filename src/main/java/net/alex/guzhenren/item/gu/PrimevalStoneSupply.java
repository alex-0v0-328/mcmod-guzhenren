package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The one supply line for primeval stones [元石]: every automatic draw on carried stones goes through here
 * -- the top-up line (refill below {@link #REFILL_BELOW_PERCENT}%, stop at {@link #REFILL_UP_TO_PERCENT}%),
 * paying a cost the pool cannot cover, the refinement menu's supply slot, and a tended Gu refilling from an
 * offhand stack while it channels. {@link #takeFrom} is the single way a stone leaves a stack, whether the
 * stack is stones or a Primeval Elder Gu [元老蛊] vault.
 *
 * <p>☠ A second copy of the top-up line drifts, and the pool silently clamps whatever a caller pours past
 * the cap. ☠ {@link #spend}: a lump bigger than the pool burns stones straight, never through the cap --
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

    public static long essencePerStone() {
        return ModItems.PRIMEVAL_STONE.get() instanceof PrimevalStoneItem stone ? stone.essence() : 0L;
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
    public static boolean needsTopUp(Player player) {
        long max = ApertureEssenceService.getMaxEssence(player);
        return max > 0L && ApertureEssenceService.getCurrentEssence(player) * 100L < max * REFILL_BELOW_PERCENT;
    }

    public static long topUpDeficit(Player player) {
        return ApertureEssenceService.getMaxEssence(player) * REFILL_UP_TO_PERCENT / 100L
                - ApertureEssenceService.getCurrentEssence(player);
    }

    public static void topUp(ServerPlayer player) {
        if (needsTopUp(player)) pourInto(player, topUpDeficit(player));
    }
    //endregion

    //region sourcing stones for something else -- the walk lives here, not in a service
    public static void pourInto(ServerPlayer player, long wanted) {
        long each = essencePerStone();
        if (wanted <= 0L || each <= 0L) return;
        int taken = draw(player, (int) Math.min(Integer.MAX_VALUE, (wanted + each - 1) / each));
        if (taken > 0) ApertureEssenceService.add(player, taken * each);
    }

    public static void fillFromOffhand(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof PrimevalStoneItem stone)) return;

        int used = stone.used(player, offhand);
        if (used <= 0) return;

        ApertureEssenceService.add(player, stone.essence() * used);
        if (!player.hasInfiniteMaterials()) offhand.shrink(used);
    }

    private static int draw(ServerPlayer player, int wanted) {
        int left = wanted;
        int taken = 0;

        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhand.getItem() instanceof PrimevalElderGuItem) {
            int drawn = takeFrom(offhand, left);
            taken += drawn;
            left -= drawn;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof PrimevalStoneItem)) continue;
            int drawn = takeFrom(stack, left);
            taken += drawn;
            left -= drawn;
        }
        return taken;
    }
    //endregion

    //region ☠ a lump bigger than the pool -- these stones burn straight, never through the cap
    public static long worthOnHand(Player player) {
        long stones = 0L;
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhand.getItem() instanceof PrimevalElderGuItem) stones += PrimevalElderGuItem.stored(offhand);

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PrimevalStoneItem) stones += stack.getCount();
        }
        return stones * essencePerStone();
    }

    public static boolean canAfford(Player player, long cost) {
        return ApertureEssenceService.spendable(player) + worthOnHand(player) >= cost;
    }

    public static boolean spend(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        long each = essencePerStone();
        if (each <= 0L || !canAfford(player, cost)) return false;

        long fromPool = Math.min(ApertureEssenceService.spendable(player), cost);
        ApertureEssenceService.consume(player, fromPool);

        long owed = cost - fromPool;
        if (owed <= 0L) return true;
        long drawn = draw(player, (int) Math.min(Integer.MAX_VALUE, (owed + each - 1) / each)) * each;
        if (drawn > owed) ApertureEssenceService.add(player, drawn - owed);
        return true;
    }
    //endregion
}
