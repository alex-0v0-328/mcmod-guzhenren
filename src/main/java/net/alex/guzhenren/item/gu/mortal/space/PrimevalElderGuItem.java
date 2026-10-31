package net.alex.guzhenren.item.gu.mortal.space;

import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Primeval Elder Gu [元老蛊]: a vault for primeval stones [元石] that pays its own upkeep from the vault.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem} but declares no clock and never eats.
 * A tapped right click deposits every carried stone (free); a held one withdraws a stack of 64 (free). The
 * stored-stones total rides {@link net.alex.guzhenren.registry.item.ModDataComponents}, not
 * {@code RefinedGuState}, because every tended Gu shares that record.
 *
 * <p>Carried in any slot, its stock is the stone supply's second source, after loose stones
 * ({@code PrimevalStoneSupply}); its own uses are free, so they never top the pool up from it.
 *
 * <p>⚠ Starving is unreachable for it by design -- a failed refinement is the only thing that can
 * destroy one. Do not "fix" the missing hunger bar.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class PrimevalElderGuItem extends TendedGuItem {

    private static final String FAILED_EMPTY = "guzhenren.item.failed.elder_gu_empty";
    private static final String FAILED_FULL = "guzhenren.item.failed.elder_gu_full";
    private static final String FAILED_NO_STONES = "guzhenren.item.failed.elder_gu_no_stones";
    private static final String TOOLTIP_STORED = "guzhenren.item.gu.stored_stones";
    private static final int WITHDRAW_STONES = 64;
    private final long capacity;

    public PrimevalElderGuItem(Properties properties, long capacity, GuSpec spec) {
        super(properties, spec);
        this.capacity = capacity;
    }

    //region the vault
    public static long getStored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_STONES, 0L);
    }

    private void setStored(ItemStack stack, long value) {
        stack.set(ModDataComponents.STORED_STONES, Math.clamp(value, 0L, capacity));
    }

    private static boolean isStone(ItemStack stack) { return stack.is(ModItems.PRIMEVAL_STONE.get()); }
    //endregion

    //region depositing -- the tap, free
    @Override
    protected boolean hasTapUse(Player player, ItemStack stack) { return isRefined(stack); }

    @Override
    protected @Nullable Refusal checkOwnTapUse(Player player, ItemStack stack) {
        if (getStored(stack) >= capacity) return new Refusal(FAILED_FULL);
        return getDepositable(player, stack) <= 0 ? new Refusal(FAILED_NO_STONES) : null;
    }

    @Override
    protected int performTapUse(ServerPlayer player, ItemStack stack) {
        deposit(player, stack);
        payOwnUpkeep(player, stack);
        return 0;
    }

    private int getDepositable(Player player, ItemStack stack) {
        long room = capacity - getStored(stack);
        if (room <= 0) return 0;

        int carried = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isStone(inventory.getItem(slot))) carried += inventory.getItem(slot).getCount();
        }
        return (int) Math.min(carried, room);
    }

    private void deposit(ServerPlayer player, ItemStack stack) {
        int wanted = getDepositable(player, stack);
        Inventory inventory = player.getInventory();

        for (int slot = 0, taken = 0; slot < inventory.getContainerSize() && taken < wanted; slot++) {
            ItemStack stones = inventory.getItem(slot);
            if (!isStone(stones)) continue;

            int move = Math.min(stones.getCount(), wanted - taken);
            if (!player.getAbilities().instabuild) stones.shrink(move);
            taken += move;
        }
        setStored(stack, getStored(stack) + wanted);
    }
    //endregion

    //region withdrawing -- the hold, which is this Gu's use
    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        return getStored(stack) <= 0 ? new Refusal(FAILED_EMPTY) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        int taken = (int) Math.min(WITHDRAW_STONES, getStored(stack));
        if (taken <= 0) return;

        setStored(stack, getStored(stack) - taken);
        ItemStack stones = new ItemStack(ModItems.PRIMEVAL_STONE.get(), taken);
        if (player.getOffhandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, stones);
            return;
        }
        player.getInventory().placeItemBackInInventory(stones);
    }
    //endregion

    //region its own vault -- it never eats, but a wound still costs stones, on the heartbeat
    @Override
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {
        if (isRefined(stack)) heal(stack, drawStones(stack, getState(stack).damageTaken()));
    }

    public int drawStones(ItemStack stack, int wanted) {
        int taken = (int) Math.min(getStored(stack), Math.max(wanted, 0L));
        if (taken > 0) setStored(stack, getStored(stack) - taken);
        return taken;
    }

    public int storeStones(ItemStack stack, int amount) {
        int room = (int) Math.min(capacity - getStored(stack), Math.max(amount, 0L));
        if (room > 0) setStored(stack, getStored(stack) + room);
        return room;
    }
    //endregion

    @Override
    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) {
        return isRefined(stack)
                ? Component.translatable(TOOLTIP_STORED, getStored(stack), capacity)
                : super.formatProgressLine(stack);
    }
}
