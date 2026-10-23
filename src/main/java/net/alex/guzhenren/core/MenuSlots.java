package net.alex.guzhenren.core;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The parts every container menu of the mod repeats around its own slots.
 *
 * <p>{@link #addPlayerInventory} lays out the player's bag in vanilla order -- the three bag rows first, then the
 * hotbar -- so the menu's slot indices stay where its shift-click routing expects them. {@link #quickMove} is the
 * shift-click frame: the menu passes only where a stack goes, and the frame returns the stack as it was before
 * the move, or empty when nothing moved, as vanilla's loop over {@code quickMoveStack} requires.
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
