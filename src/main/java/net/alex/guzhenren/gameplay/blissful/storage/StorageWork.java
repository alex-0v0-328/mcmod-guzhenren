package net.alex.guzhenren.gameplay.blissful.storage;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 库藏页的存取：取一组到光标或进背包、取满背包、存光标物品、把背包里库藏已有的同类一键存入。
 * <ul>
 *   <li>⚠ 库藏下标来自客户端快照，可能已过时：取物同时带物品 ID，下标处不是这种物品就改取第一条同 ID 的条目，没有就不
 *       取。取进背包前先算背包还放得下多少（{@code room}），只取这么多：创造模式背包满时 {@code Inventory#add} 会把数量
 *       吞成 0 并报成功。</li>
 *   <li>{@link #depositMatching} 只动背包主格（不含快捷栏），免得把手上的工具存进去；只存库藏里已有的「物品 + 组件」。
 *   </li>
 * </ul>
 */

public final class StorageWork {

    private StorageWork() {}

    public static @NotNull String idOf(@NotNull ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static void take(@NotNull ServerPlayer player, @NotNull EarthSpiritMenu menu,
                            @NotNull BlissfulStorage storage, int index, @NotNull String itemId, boolean all) {
        int at = resolve(storage, index, itemId);
        if (at < 0) return;

        ItemStack key = storage.entries().get(at).key().copy();
        if (!all && menu.getCarried().isEmpty()) {
            menu.setCarried(storage.extract(at, key.getMaxStackSize()));
            return;
        }
        int room = room(player.getInventory(), key);
        int wanted = all ? room : Math.min(room, key.getMaxStackSize());
        while (wanted > 0) {
            int next = indexOf(storage, key);
            if (next < 0) break;
            ItemStack stack = storage.extract(next, Math.min(wanted, key.getMaxStackSize()));
            wanted -= stack.getCount();
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                storage.insert(stack);
                break;
            }
        }
    }

    public static void depositCarried(@NotNull EarthSpiritMenu menu, @NotNull BlissfulStorage storage) {
        if (storage.insert(menu.getCarried())) menu.setCarried(ItemStack.EMPTY);
    }

    public static int depositMatching(@NotNull ServerPlayer player, @NotNull BlissfulStorage storage) {
        Inventory inventory = player.getInventory();
        int moved = 0;
        for (int slot = Inventory.getSelectionSize(); slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.isEmpty() || indexOf(storage, stack) < 0) continue;
            int count = stack.getCount();
            if (storage.insert(stack)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                moved += count;
            }
        }
        return moved;
    }

    private static int resolve(BlissfulStorage storage, int index, String itemId) {
        List<BlissfulStorage.Entry> entries = storage.entries();
        if (index >= 0 && index < entries.size() && idOf(entries.get(index).key()).equals(itemId)) return index;
        for (int i = 0; i < entries.size(); i++) {
            if (idOf(entries.get(i).key()).equals(itemId)) return i;
        }
        return -1;
    }

    private static int room(Inventory inventory, ItemStack key) {
        int room = 0;
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) {
                room += key.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, key)) {
                room += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
        }
        return room;
    }

    private static int indexOf(BlissfulStorage storage, ItemStack key) {
        List<BlissfulStorage.Entry> entries = storage.entries();
        for (int i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).key(), key)) return i;
        }
        return -1;
    }
}
