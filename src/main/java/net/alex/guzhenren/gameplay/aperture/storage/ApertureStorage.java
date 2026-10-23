package net.alex.guzhenren.gameplay.aperture.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import net.alex.guzhenren.core.ItemStacks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The Gu kept inside each Aperture [空窍], including the Vital Gu [本命蛊] bound to that aperture.
 * Immutable record attachment keyed {@code aperture_storage}; serialized but NOT synced -- every reader
 * is server-side and the menu reads it through slot channels; {@code
 * ApertureStorageService} is the only runtime writer.
 *
 * <p>⚠ Uses {@link ItemStacks#OPTIONAL_CODEC}: an interior empty is a real slot position, only TRAILING
 * holes are trimmed, or items would jump the moment a gap is saved. ⚠ {@code with} GROWS to reach its
 * index (unlike {@link ApertureData#with}, which refuses) -- a store may exist before its aperture opens.
 * ⚠ {@link #view} hands out the stored stacks themselves, not copies, for a read-only walk: copy a stack
 * before changing it.
 *
 * <p>A record owns its stacks. Lists from outside are deep-copied on construction, while {@link #with},
 * {@link #withPage}, {@link #withVital} and {@link #shiftRight} hand the next record the stacks this one already
 * holds (the private {@code OwnedStacks}) and copy only a stack that is new at its position, so writing one changed
 * slot back costs one copy instead of the whole store.
 *
 * <p>{@link #shiftRight} makes room for a first aperture inserted ahead of a lone second one: every
 * stored list and the Vital Gu slot moves one position up. The Vital Gu that moves is the second
 * aperture's, so it is pinned to kind {@code SECOND} explicitly: one bound by the component default
 * ({@code PRIMARY}) would otherwise read as the new first aperture's.
 *
 * <p>{@link #tokens} holds, per aperture kind, the token of the one current Vital Gu: an aperture has one
 * Vital Gu, so a replaced one whose copy still carries the mark is not it ({@code GuItem.isCurrentVital}).
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
            ItemStacks.OPTIONAL_CODEC.listOf().listOf().optionalFieldOf("by_aperture", List.of())
                    .forGetter(ApertureStorage::byAperture),
            ItemStacks.OPTIONAL_CODEC.listOf().optionalFieldOf("vital", List.of())
                    .forGetter(ApertureStorage::vital),
            Codec.LONG.listOf().optionalFieldOf("vital_tokens", List.of())
                    .forGetter(ApertureStorage::tokens)
    ).apply(instance, ApertureStorage::new));

    public ApertureStorage {
        List<List<ItemStack>> kept = new ArrayList<>();
        for (int i = 0; i < Math.min(byAperture.size(), ApertureData.MAX_APERTURES); i++) {
            List<ItemStack> items = own(byAperture.get(i), Integer.MAX_VALUE);
            while (!items.isEmpty() && items.get(items.size() - 1).isEmpty()) items.remove(items.size() - 1);
            kept.add(new OwnedStacks(items));
        }
        while (!kept.isEmpty() && kept.get(kept.size() - 1).isEmpty()) kept.remove(kept.size() - 1);
        byAperture = Collections.unmodifiableList(kept);

        List<ItemStack> bound = own(vital, ApertureData.MAX_APERTURES);
        while (!bound.isEmpty() && bound.get(bound.size() - 1).isEmpty()) bound.remove(bound.size() - 1);
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

    public List<ItemStack> view(int aperture) {
        return aperture >= 0 && aperture < byAperture.size() ? byAperture.get(aperture) : List.of();
    }

    public ItemStack getVital(int aperture) {
        return aperture >= 0 && aperture < vital.size() ? vital.get(aperture).copy() : ItemStack.EMPTY;
    }

    public int count(int aperture) {
        return aperture >= 0 && aperture < byAperture.size() ? byAperture.get(aperture).size() : 0;
    }

    public List<ItemStack> page(int aperture, int from, int size) {
        if (aperture < 0 || aperture >= byAperture.size() || from < 0 || size <= 0) {
            return List.of();
        }
        List<ItemStack> stored = byAperture.get(aperture);
        int to = Math.min(stored.size(), from + size);
        return Collections.unmodifiableList(copyStacks(stored.subList(Math.min(from, to), to)));
    }

    public boolean matchesPage(int aperture, int from, List<ItemStack> page) {
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
                    || !ItemStacks.sameItemSameData(wanted, current)) return false;
        }
        return true;
    }

    public ApertureStorage with(int aperture, List<ItemStack> items) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> next = new ArrayList<>(byAperture);
        while (next.size() <= aperture) next.add(List.of());
        next.set(aperture, keepOwned(view(aperture), items));
        return new ApertureStorage(next, vital, tokens);
    }

    public ApertureStorage withSlot(int aperture, int index, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(view(aperture));
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
        next.set(aperture, keepOwned(view(aperture), all));
        return new ApertureStorage(next, vital, tokens);
    }

    public ApertureStorage withVital(int aperture, ItemStack stack) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<ItemStack> next = new ArrayList<>(vital);
        while (next.size() <= aperture) next.add(ItemStack.EMPTY);
        next.set(aperture, stack);
        return new ApertureStorage(byAperture, keepOwned(vital, next), tokens);
    }

    public ApertureStorage shiftRight() {
        if (byAperture.size() >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> items = new ArrayList<>();
        items.add(List.of());
        items.addAll(byAperture);

        List<ItemStack> bound = new ArrayList<>();
        bound.add(ItemStack.EMPTY);
        for (ItemStack stack : vital) bound.add(rebound(stack));
        return new ApertureStorage(items, new OwnedStacks(bound), tokens);
    }

    private static ItemStack rebound(ItemStack stack) {
        if (stack.isEmpty()) return stack;

        ItemStack copy = stack.copy();
        ModDataComponents.set(copy, ModDataComponents.VITAL_APERTURE, ApertureData.SECOND);
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

    private static List<ItemStack> own(List<ItemStack> stacks, int limit) {
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
