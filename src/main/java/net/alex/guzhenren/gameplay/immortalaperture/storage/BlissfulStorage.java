package net.alex.guzhenren.gameplay.immortalaperture.storage;

import com.google.common.math.LongMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 福地库藏：地灵收来的产物与主人存入的物品，按「物品 + 组件」归并成条目，每条数量为 long，不设上限。
 * <ul>
 *   <li>条目键是数量为 1 的 {@link ItemStack} 副本，归并用 {@link ItemStack#isSameItemSameComponents}；条目按首次入库顺
 *       序排列，数量归零即删。</li>
 *   <li>每条有本次开服内唯一的流水号（{@link Entry#serial}，不存档，读档时重排）：面板取物带流水号，快照过时、下标挪
 *       动也拿不错；条目删掉后再入库的同种物品换新号，旧号取不到东西。</li>
 *   <li>{@link #revision} 每次改动加一，面板浏览据此缓存筛选排序结果。</li>
 *   <li>胆识蛊[Guts Gu]不入库（{@link #accepts}），否则可借库藏把它带出荡魂山。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code storage}），任何改动经回调通知所属存档置脏。</li>
 *   <li>⚠ 性能：查找是线性的；条目数等于物品种类数，福地产物只有几十种。</li>
 * </ul>
 */

public final class BlissfulStorage implements LandSavePart.State {

    private final List<Entry> entries = new ArrayList<>();
    private final Runnable changed;
    private long nextSerial;
    private long revision;

    public BlissfulStorage(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull BlissfulStorage of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public static final LandSavePart<BlissfulStorage> PART = new LandSavePart<>("storage", BlissfulStorage::new);
    public static final class Entry {

        private final long serial;
        private final ItemStack key;
        private long count;

        private Entry(long serial, ItemStack key, long count) {
            this.serial = serial;
            this.key = key;
            this.count = count;
        }

        public long serial() { return serial; }

        public @NotNull ItemStack key() { return key; }

        public long count() { return count; }
    }

    public @NotNull List<Entry> entries() { return Collections.unmodifiableList(entries); }

    public long revision() { return revision; }

    public int indexOf(long serial) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).serial == serial) return i;
        }
        return -1;
    }

    private void touch() {
        revision++;
        changed.run();
    }

    public static boolean accepts(@NotNull ItemStack stack) {
        return !stack.isEmpty() && !SoulsunderMountain.isGutsGu(stack);
    }

    public boolean insert(@NotNull ItemStack stack) {
        if (!accepts(stack)) return false;

        for (Entry entry : entries) {
            if (ItemStack.isSameItemSameComponents(entry.key, stack)) {
                entry.count = LongMath.saturatedAdd(entry.count, stack.getCount());
                touch();
                return true;
            }
        }
        entries.add(new Entry(nextSerial++, stack.copyWithCount(1), stack.getCount()));
        touch();
        return true;
    }

    public long count(@NotNull Predicate<ItemStack> filter) {
        long total = 0;
        for (Entry entry : entries) {
            if (filter.test(entry.key)) total = LongMath.saturatedAdd(total, entry.count);
        }
        return total;
    }

    public @NotNull ItemStack first(@NotNull Predicate<ItemStack> filter) {
        for (Entry entry : entries) {
            if (filter.test(entry.key)) return entry.key.copy();
        }
        return ItemStack.EMPTY;
    }

    public int consume(@NotNull Predicate<ItemStack> filter, int amount) {
        int taken = 0;
        for (int i = 0; i < entries.size() && taken < amount; ) {
            Entry entry = entries.get(i);
            if (!filter.test(entry.key)) {
                i++;
                continue;
            }
            int part = (int) Math.min(entry.count, amount - taken);
            entry.count -= part;
            taken += part;
            if (entry.count == 0) entries.remove(i);
            else i++;
        }
        if (taken > 0) touch();
        return taken;
    }

    public @NotNull ItemStack extract(int index, int max) {
        if (index < 0 || index >= entries.size() || max <= 0) return ItemStack.EMPTY;

        Entry entry = entries.get(index);
        int count = (int) Math.min(Math.min(entry.count, max), entry.key.getMaxStackSize());
        entry.count -= count;
        if (entry.count == 0) entries.remove(index);
        touch();
        return entry.key.copyWithCount(count);
    }

    @Override
    public void save(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag item = new CompoundTag();
            item.put("item", stacks.save(entry.key));
            item.putLong("count", entry.count);
            list.add(item);
        }
        tag.put(key, list);
    }

    @Override
    public void load(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks) {
        entries.clear();
        revision++;
        for (Tag element : tag.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) element;
            ItemStack stack = stacks.load(item.getCompound("item"));
            long count = item.getLong("count");
            if (!stack.isEmpty() && count > 0) entries.add(new Entry(nextSerial++, stack.copyWithCount(1), count));
        }
    }
}
