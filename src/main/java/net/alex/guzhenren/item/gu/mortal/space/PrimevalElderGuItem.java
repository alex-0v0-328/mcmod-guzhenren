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
 * Right click deposits every stone (free); sneak + right click withdraws a stack of 64 (free). The
 * stored-stones total rides {@link net.alex.guzhenren.registry.item.ModDataComponents}, not
 * {@code RefinedGuState}, because every tended Gu shares that record.
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
    private static final long DEPOSIT_IS_FREE = 0L;
    private final long capacity;

    public PrimevalElderGuItem(Properties properties, long capacity, GuSpec spec) {
        super(properties, spec);
        this.capacity = capacity;
    }

    //region the vault
    public static long stored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_STONES.get(), 0L);
    }

    private void setStored(ItemStack stack, long value) {
        stack.set(ModDataComponents.STORED_STONES.get(), Math.clamp(value, 0L, capacity));
    }

    @Override
    protected int feedUnits(ItemStack food) { return isStone(food) ? 1 : 0; }

    @Override
    protected boolean holdingFood(Player player, ItemStack stack) { return false; }

    private static boolean isStone(ItemStack stack) { return stack.is(ModItems.PRIMEVAL_STONE.get()); }
    //endregion

    //region depositing -- the plain right click, free and instant
    @Override
    protected long useThreshold(ItemStack stack) { return DEPOSIT_IS_FREE; }

    @Override
    protected @Nullable Refusal useGate(Player player, ItemStack stack) {
        if (stored(stack) >= capacity) return new Refusal(FAILED_FULL);
        return depositable(player, stack) <= 0 ? new Refusal(FAILED_NO_STONES) : null;
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        deposit(player, stack);
        payOwnUpkeep(player, stack);
        return 0;
    }

    private int depositable(Player player, ItemStack stack) {
        long room = capacity - stored(stack);
        if (room <= 0) return 0;

        int carried = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isStone(inventory.getItem(slot))) carried += inventory.getItem(slot).getCount();
        }
        return (int) Math.min(carried, room);
    }

    private void deposit(ServerPlayer player, ItemStack stack) {
        int wanted = depositable(player, stack);
        Inventory inventory = player.getInventory();

        for (int slot = 0, taken = 0; slot < inventory.getContainerSize() && taken < wanted; slot++) {
            ItemStack stones = inventory.getItem(slot);
            if (!isStone(stones)) continue;

            int move = Math.min(stones.getCount(), wanted - taken);
            if (!player.hasInfiniteMaterials()) stones.shrink(move);
            taken += move;
        }
        setStored(stack, stored(stack) + wanted);
    }
    //endregion

    //region withdrawing -- sneak + right click, which is this Gu's use
    @Override
    protected boolean hasSneakUse(Player player, ItemStack stack) { return refined(stack); }

    @Override
    protected @Nullable Refusal sneakGate(Player player, ItemStack stack) {
        Refusal poor = essenceGate(player, spec.essencePerRound(), FAILED_ESSENCE);
        return poor != null ? poor : payoutGate(player, stack);
    }

    @Override
    protected int sneakApply(ServerPlayer player, ItemStack stack) { return drive(player, stack); }

    @Override
    protected int useChargeTicks(Player player, ItemStack stack) {
        return isSneakUse(player, stack) ? super.useChargeTicks(player, stack) : 0;
    }

    @Override
    protected @Nullable Refusal payoutGate(Player player, ItemStack stack) {
        return stored(stack) <= 0 ? new Refusal(FAILED_EMPTY) : null;
    }

    @Override
    protected void payout(ServerPlayer player, ItemStack stack) {
        int taken = (int) Math.min(WITHDRAW_STONES, stored(stack));
        if (taken <= 0) return;

        setStored(stack, stored(stack) - taken);
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
        if (refined(stack)) heal(stack, drawStones(stack, state(stack).damageTaken()));
    }

    public int drawStones(ItemStack stack, int wanted) {
        int taken = (int) Math.clamp(wanted, 0, stored(stack));
        if (taken > 0) setStored(stack, stored(stack) - taken);
        return taken;
    }

    public int storeStones(ItemStack stack, int amount) {
        int room = (int) Math.clamp(amount, 0, capacity - stored(stack));
        if (room > 0) setStored(stack, stored(stack) + room);
        return room;
    }
    //endregion

    @Override
    protected @Nullable MutableComponent progressLine(ItemStack stack) {
        return refined(stack)
                ? Component.translatable(TOOLTIP_STORED, stored(stack), capacity)
                : super.progressLine(stack);
    }
}
