package net.alex.guzhenren.core;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 模组各容器菜单在自身槽位外重复的部分。
 * <ul>
 *   <li>{@link #addPlayerInventory}：按原版顺序先三行背包后快捷栏，槽位下标才合 shift 点击路由。</li>
 *   <li>{@link #quickMove}：shift 点击框架，菜单只给去向；返回移动前堆叠，未动返回空（原版 {@code quickMoveStack} 循环
 *       所需）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
