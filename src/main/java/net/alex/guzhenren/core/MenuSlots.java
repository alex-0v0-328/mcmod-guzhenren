package net.alex.guzhenren.core;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 模组容器菜单共用的玩家背包槽位布局与 Shift 快速移动辅助工具。
 * <ul>
 *   <li>{@link #addPlayerInventory} 严格按原版顺序先添加 3 行主背包槽位（下标 9–35）、再添加底部快捷栏槽位（下标 0–
 *       8），保证容器槽位索引与 Shift 点击路由约定一致。</li>
 *   <li>{@link #quickMove} 封装原版 {@code quickMoveStack} 的样板流程，由具体菜单传入目标区间移动谓词；移动成功时返回移
 *       动前的堆叠副本，未发生移动时返回 {@link ItemStack#EMPTY} 以终止原版的快速移动循环。</li>
 * </ul>
 */

public final class MenuSlots {

    private static final int BAG_ROWS = 3;

    private MenuSlots() {}

    public static void addPlayerInventory(@NotNull Consumer<Slot> add, @NotNull Inventory inventory, int left,
            int top, int hotbarTop, int slotSize) {
        int columns = Inventory.getSelectionSize();
        for (int row = 0; row < BAG_ROWS; row++) {
            for (int col = 0; col < columns; col++) {
                add.accept(new Slot(inventory, col + row * columns + columns,
                        left + col * slotSize, top + row * slotSize));
            }
        }
        for (int col = 0; col < columns; col++) {
            add.accept(new Slot(inventory, col, left + col * slotSize, hotbarTop));
        }
    }

    public static @NotNull ItemStack quickMove(@NotNull Slot slot, @NotNull Predicate<ItemStack> move) {
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!move.test(stack)) return ItemStack.EMPTY;

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
