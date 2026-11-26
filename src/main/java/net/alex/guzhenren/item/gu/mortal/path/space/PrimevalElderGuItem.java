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
 * 元老蛊[Primeval Elder Gu]物品：随身存储元石[Primeval Stone]的持续照顾型蛊[Tended Gu]。
 * <ul>
 *   <li>炼化后主手右键单击即免费将背包内全部元石存入蛊中（无蓄力条）；取出元石不走常规催动：客户端长按滑条
 *       （{@code PrimevalElderInput}）选定组数后，服务端 {@link #tryWithdraw} 免费取出至多 {@code stacks} 组（每组 64
 *       块）放入主背包（被拒或处于原版物品冷却时返回 {@code false}）；存取均仅限主手，野生状态下仍为长按炼化。</li>
 *   <li>创造模式下催动不检查随身元石，直接将内部存量填满至容量上限且不改动背包（已满时照常拒绝）。</li>
 *   <li>⚠ {@link #tryWithdraw} 仅填充主背包 36 格，优先补齐未满的普通元石堆（跳过重命名等带额外组件的元石），绝不超出
 *       {@link #getBagRoom}、不塞入副手、也不向地面抛洒；客户端滑条上限 {@link #getWithdrawableStacks} 读取同一剩余空
 *       间。</li>
 *   <li>元石存量记录在独立组件 {@link ModDataComponents#STORED_STONES} 中，不放入 {@code RefinedGuState}；无论放在背包
 *       何处，已炼化的元老蛊均作为 {@code PrimevalStoneSupply} 的第二顺位自动补给源（排在散装元石之后）。</li>
 *   <li>⚠ 元老蛊不声明饱食时钟、不进食，按设计绝不会饿死（仅炼化失败会毁掉它，切勿为其补加饱食条）；伤势同普通蛊，无
 *       恢复手段，库存元石不再用来修伤。</li>
 * </ul>
 */

public class PrimevalElderGuItem extends TendedGuItem {

    public static final int STACK = 64;
    private static final String FAILED_EMPTY = "guzhenren.item.failed.primeval_elder_gu_empty";
    private static final String FAILED_FULL = "guzhenren.item.failed.primeval_elder_gu_full";
    private static final String FAILED_NO_STONES = "guzhenren.item.failed.primeval_elder_gu_no_stones";
    private static final String TOOLTIP_STORED = "guzhenren.item.gu.stored_stones";
    private final long capacity;

    public PrimevalElderGuItem(long capacity, GuProperties guProperties) {
        super(guProperties);
        this.capacity = capacity;
    }

    //region the vault [元石库]
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

    //region depositing [存入]
    @Override
    protected boolean hasOffhandUse() { return false; }

    @Override
    protected int useChargeTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        if (getStored(stack) >= capacity) return new Refusal(FAILED_FULL);
        if (player.getAbilities().instabuild) return null;
        return getDepositable(player, stack) <= 0 ? new Refusal(FAILED_NO_STONES) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) setStored(stack, capacity);
        else deposit(player, stack);
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
            stones.shrink(move);
            taken += move;
        }
        setStored(stack, getStored(stack) + wanted);
    }
    //endregion

    //region withdrawing [取出]
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

    public boolean tryWithdraw(ServerPlayer player, ItemStack stack, int stacks) {
        if (stacks <= 0 || !isRefined(stack) || player.getCooldowns().isOnCooldown(this)) return false;
        if (getStored(stack) <= 0) {
            ActionBar.refuse(player, FAILED_EMPTY);
            return false;
        }
        int room = getBagRoom(player);
        if (room <= 0) {
            ActionBar.refuse(player, Refusal.BAG_FULL);
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

    //region the stock [库存]
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
