package net.alex.guzhenren.gameplay.aperture.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.minecraft.world.item.ItemStack;

/**
 * 各空窍[Aperture]所存蛊，含绑定该空窍的本命蛊；附件 {@code aperture_storage}。
 * <ul>
 *   <li>存档不同步：读者全在服务端，菜单经槽位通道读；运行时仅 {@code ApertureStorageService} 写。</li>
 *   <li>⚠ 用 {@link ItemStack#OPTIONAL_CODEC}：中间空位是真槽位，只裁尾部空位，否则存档后隔空物品跳位。</li>
 *   <li>⚠ {@code with} 扩到所需下标（{@link ApertureData#with} 拒绝）：存储可先于空窍存在。</li>
 *   <li>⚠ {@link #getView} 交出栈本身而非副本，仅供只读遍历，改前先复制。</li>
 *   <li>自有其栈：外来列表深拷贝，各 {@code with*} 与 {@link #shiftRight} 沿用已有栈（私有 {@code OwnedStacks}），
 *       只拷新栈，写一格只拷一次。</li>
 *   <li>{@link #shiftRight} 为新第一空窍腾位，各列表与本命蛊槽上移一位；上移本命蛊属第二空窍，经调用方的
 *       {@link StoredGu} 钉为 {@code SECOND}，否则按默认 {@code PRIMARY} 归新第一空窍。</li>
 *   <li>{@link #tokens} 按空窍种类存现任本命蛊令牌，旧副本带标记也不算（{@code VitalGuState.isCurrentVital}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureData
 * @see ApertureStorageService
 * @since 1.0.0
 */

public record ApertureStorage(List<List<ItemStack>> byAperture, List<ItemStack> vital, List<Long> tokens) {

    public static final ApertureStorage DEFAULT = new ApertureStorage(List.of(), List.of(), List.of());
    public static final Codec<ApertureStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().listOf().optionalFieldOf("by_aperture", List.of())
                    .forGetter(ApertureStorage::byAperture),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("vital", List.of())
                    .forGetter(ApertureStorage::vital),
            Codec.LONG.listOf().optionalFieldOf("vital_tokens", List.of())
                    .forGetter(ApertureStorage::tokens)
    ).apply(instance, ApertureStorage::new));

    public ApertureStorage {
        List<List<ItemStack>> kept = new ArrayList<>();
        for (int i = 0; i < Math.min(byAperture.size(), ApertureData.MAX_APERTURES); i++) {
            List<ItemStack> items = copyOwned(byAperture.get(i), Integer.MAX_VALUE);
            while (!items.isEmpty() && items.getLast().isEmpty()) items.removeLast();
            kept.add(new OwnedStacks(items));
        }
        while (!kept.isEmpty() && kept.getLast().isEmpty()) kept.removeLast();
        byAperture = Collections.unmodifiableList(kept);

        List<ItemStack> bound = copyOwned(vital, ApertureData.MAX_APERTURES);
        while (!bound.isEmpty() && bound.getLast().isEmpty()) bound.removeLast();
        vital = new OwnedStacks(bound);
        tokens = List.copyOf(tokens.subList(0, Math.min(tokens.size(), ApertureData.MAX_APERTURES)));
    }

    public ApertureStorage(List<List<ItemStack>> byAperture, List<ItemStack> vital) {
        this(byAperture, vital, List.of());
    }

    public List<ItemStack> get(int aperture) {
        return aperture >= 0 && aperture < byAperture.size()
                ? Collections.unmodifiableList(copyStacks(byAperture.get(aperture))) : List.of();
    }

    public List<ItemStack> getView(int aperture) {
        return aperture >= 0 && aperture < byAperture.size() ? byAperture.get(aperture) : List.of();
    }

    public ItemStack getVital(int aperture) {
        return aperture >= 0 && aperture < vital.size() ? vital.get(aperture).copy() : ItemStack.EMPTY;
    }

    public int count(int aperture) {
        return aperture >= 0 && aperture < byAperture.size() ? byAperture.get(aperture).size() : 0;
    }

    public List<ItemStack> getPage(int aperture, int from, int size) {
        if (aperture < 0 || aperture >= byAperture.size() || from < 0 || size <= 0) {
            return List.of();
        }
        List<ItemStack> stored = byAperture.get(aperture);
        int to = Math.min(stored.size(), from + size);
        return Collections.unmodifiableList(copyStacks(stored.subList(Math.min(from, to), to)));
    }

    public boolean isSamePage(int aperture, int from, List<ItemStack> page) {
        if (from < 0) return false;
        if (aperture < 0 || aperture >= byAperture.size()) {
            for (ItemStack stack : page) if (!stack.isEmpty()) return false;
            return true;
        }

        List<ItemStack> stored = byAperture.get(aperture);
        for (int i = 0; i < page.size(); i++) {
            ItemStack current = from + i < stored.size() ? stored.get(from + i) : ItemStack.EMPTY;
            ItemStack wanted = page.get(i);
            if (wanted.getCount() != current.getCount()
                    || !ItemStack.isSameItemSameComponents(wanted, current)) return false;
        }
        return true;
    }

    public ApertureStorage with(int aperture, List<ItemStack> items) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> next = new ArrayList<>(byAperture);
        while (next.size() <= aperture) next.add(List.of());
        next.set(aperture, keepOwned(getView(aperture), items));
        return new ApertureStorage(next, vital, tokens);
    }

    public ApertureStorage withSlot(int aperture, int index, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(getView(aperture));
        items.set(index, stack);
        return with(aperture, items);
    }

    public ApertureStorage withPage(int aperture, int from, List<ItemStack> page) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES || from < 0) return this;

        List<List<ItemStack>> next = new ArrayList<>(byAperture);
        while (next.size() <= aperture) next.add(List.of());
        List<ItemStack> all = new ArrayList<>(next.get(aperture));
        while (all.size() < from + page.size()) all.add(ItemStack.EMPTY);
        for (int i = 0; i < page.size(); i++) all.set(from + i, page.get(i));
        next.set(aperture, keepOwned(getView(aperture), all));
        return new ApertureStorage(next, vital, tokens);
    }

    public ApertureStorage withVital(int aperture, ItemStack stack) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<ItemStack> next = new ArrayList<>(vital);
        while (next.size() <= aperture) next.add(ItemStack.EMPTY);
        next.set(aperture, stack);
        return new ApertureStorage(byAperture, keepOwned(vital, next), tokens);
    }

    public ApertureStorage shiftRight(StoredGu storedGu) {
        if (byAperture.size() >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> items = new ArrayList<>();
        items.add(List.of());
        items.addAll(byAperture);

        List<ItemStack> bound = new ArrayList<>();
        bound.add(ItemStack.EMPTY);
        for (ItemStack stack : vital) bound.add(markSecond(storedGu, stack));
        return new ApertureStorage(items, new OwnedStacks(bound), tokens);
    }

    private static ItemStack markSecond(StoredGu storedGu, ItemStack stack) {
        if (stack.isEmpty()) return stack;

        ItemStack copy = stack.copy();
        storedGu.setBoundAperture(copy, ApertureData.SECOND);
        return copy;
    }

    public long getToken(int kind) { return kind >= 0 && kind < tokens.size() ? tokens.get(kind) : 0L; }

    public ApertureStorage withToken(int kind, long token) {
        if (kind < 0 || kind >= ApertureData.MAX_APERTURES) return this;

        List<Long> next = new ArrayList<>(tokens);
        while (next.size() <= kind) next.add(0L);
        next.set(kind, token);
        return new ApertureStorage(byAperture, vital, next);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) copies.add(stack.copy());
        return copies;
    }

    private static List<ItemStack> copyOwned(List<ItemStack> stacks, int limit) {
        List<ItemStack> head = stacks.size() > limit ? stacks.subList(0, limit) : stacks;
        if (stacks instanceof OwnedStacks) return new ArrayList<>(head);
        return copyStacks(head);
    }

    private static OwnedStacks keepOwned(List<ItemStack> owned, List<ItemStack> incoming) {
        List<ItemStack> stacks = new ArrayList<>(incoming.size());
        for (int i = 0; i < incoming.size(); i++) {
            ItemStack stack = incoming.get(i);
            stacks.add(i < owned.size() && owned.get(i) == stack ? stack : stack.copy());
        }
        return new OwnedStacks(stacks);
    }

    private static final class OwnedStacks extends AbstractList<ItemStack> implements RandomAccess {

        private final List<ItemStack> stacks;

        private OwnedStacks(List<ItemStack> stacks) { this.stacks = stacks; }

        @Override
        public ItemStack get(int index) { return stacks.get(index); }

        @Override
        public int size() { return stacks.size(); }
    }
}
