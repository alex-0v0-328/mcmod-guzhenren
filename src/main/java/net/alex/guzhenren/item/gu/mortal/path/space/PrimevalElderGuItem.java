package net.alex.guzhenren.item.gu.mortal.path.space;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Primeval Elder Gu [元老蛊]: a vault for primeval stones [元石] that pays its own upkeep from the vault.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.tending.TendedGuItem} but declares no clock and never eats. Once refined its
 * use is the deposit: every carried stone goes in, free and with no charge, so it never shows a charge bar. The
 * withdrawal is no item use at all: the client's hold slider ({@code PrimevalElderInput}) picks a number of stacks
 * and {@link #tryWithdraw} lands it, free. Both answer only in the main hand; a wild one is still refined by holding
 * the click, with its refining bar. The stored-stones total rides
 * {@link net.alex.guzhenren.registry.item.ModDataComponents}, not {@code RefinedGuState}, because every tended Gu
 * shares that record.
 *
 * <p>⚠ A withdrawal fills the 36 bag slots only, partial plain stone stacks first (a renamed one is left alone),
 * and never past {@link #getBagRoom}: no stone goes to the offhand or the ground. The slider's top,
 * {@link #getWithdrawableStacks}, reads the same room.
 *
 * <p>Carried in any slot, its stock is the stone supply's second source, after loose stones
 * ({@code PrimevalStoneSupply}); its own uses are free, so they never top the pool up from it.
 *
 * <p>⚠ Starving is unreachable for it by design -- a failed refinement is the only thing that can
 * destroy one. Do not "fix" the missing hunger bar.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
 */

public class PrimevalElderGuItem extends TendedGuItem {

    public static final int STACK = 64;
    private static final String FAILED_EMPTY = "guzhenren.item.failed.elder_gu_empty";
    private static final String FAILED_FULL = "guzhenren.item.failed.elder_gu_full";
    private static final String FAILED_NO_STONES = "guzhenren.item.failed.elder_gu_no_stones";
    private static final String FAILED_BAG_FULL = "guzhenren.item.failed.elder_gu_bag_full";
    private static final String TOOLTIP_STORED = "guzhenren.item.gu.stored_stones";
    private final long capacity;

    public PrimevalElderGuItem(long capacity, GuProperties guProperties) {
        super(guProperties);
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

    private static boolean isPlainStone(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, new ItemStack(ModItems.PRIMEVAL_STONE.get()));
    }
    //endregion

    //region depositing -- the use, free and with no charge
    @Override
    protected boolean hasOffhandUse() { return false; }

    @Override
    protected int useChargeTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        if (getStored(stack) >= capacity) return new Refusal(FAILED_FULL);
        return getDepositable(player, stack) <= 0 ? new Refusal(FAILED_NO_STONES) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        deposit(player, stack);
        payOwnUpkeep(player, stack);
        playPickup(player);
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

    //region withdrawing -- the slider's pick, free, into the bag only
    public static int getBagRoom(Player player) {
        int room = 0;
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) room += STACK;
            else if (isPlainStone(slot)) room += Math.max(STACK - slot.getCount(), 0);
        }
        return room;
    }

    public static int getWithdrawableStacks(Player player, ItemStack stack) {
        long stones = Math.min(getStored(stack), getBagRoom(player));
        return (int) ((stones + STACK - 1) / STACK);
    }

    /** Withdraws up to {@code stacks} stacks of 64 into the bag; false when refused or on the vanilla cooldown. */
    public boolean tryWithdraw(ServerPlayer player, ItemStack stack, int stacks) {
        if (stacks <= 0 || !isRefined(stack) || player.getCooldowns().isOnCooldown(this)) return false;
        if (getStored(stack) <= 0) {
            ActionBar.refuse(player, FAILED_EMPTY);
            return false;
        }
        int room = getBagRoom(player);
        if (room <= 0) {
            ActionBar.refuse(player, FAILED_BAG_FULL);
            return false;
        }
        int taken = (int) Math.min(Math.min((long) stacks * STACK, getStored(stack)), room);
        setStored(stack, getStored(stack) - taken);
        fillBag(player, taken);
        playPickup(player);
        spend(player, stack, 0);
        return true;
    }

    private static void fillBag(Player player, int stones) {
        List<ItemStack> items = player.getInventory().items;
        int left = stones;
        for (int slot = 0; slot < items.size() && left > 0; slot++) {
            ItemStack held = items.get(slot);
            if (!isPlainStone(held)) continue;

            int move = Math.min(STACK - held.getCount(), left);
            if (move <= 0) continue;
            held.grow(move);
            left -= move;
        }
        for (int slot = 0; slot < items.size() && left > 0; slot++) {
            if (!items.get(slot).isEmpty()) continue;

            int move = Math.min(STACK, left);
            items.set(slot, new ItemStack(ModItems.PRIMEVAL_STONE.get(), move));
            left -= move;
        }
    }

    private static void playPickup(ServerPlayer player) {
        RandomSource random = player.getRandom();
        float pitch = ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F;
        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, pitch);
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
