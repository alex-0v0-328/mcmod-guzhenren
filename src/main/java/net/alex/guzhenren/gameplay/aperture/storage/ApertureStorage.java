package net.alex.guzhenren.gameplay.aperture.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The Gu kept inside each Aperture [空窍], including the Vital Gu [本命蛊] bound to that aperture.
 * Immutable record attachment keyed {@code aperture_storage}; serialized but NOT synced -- every reader
 * is server-side and the menu reads it through slot channels; {@link
 * ApertureStorageService} is the only runtime writer.
 *
 * <p>⚠ Uses {@code ItemStack.OPTIONAL_CODEC}: an interior empty is a real slot position, only TRAILING
 * holes are trimmed, or items would jump the moment a gap is saved. ⚠ {@code with} GROWS to reach its
 * index (unlike {@link ApertureData#with}, which refuses) -- a store may exist before its aperture opens.
 * ⚠ {@link #view} hands out the stored stacks themselves, not copies, for a read-only walk: copy a stack
 * before changing it.
 *
 * <p>{@link #shiftRight} makes room for a first aperture inserted ahead of a lone second one: every
 * stored list and the Vital Gu slot moves one position up. A Vital Gu bound by the component default
 * (aperture 0) is pinned to 1 explicitly, or it would silently follow the new first aperture.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureData
 * @see ApertureStorageService
 * @since 1.0.0
 */

public record ApertureStorage(List<List<ItemStack>> byAperture, List<ItemStack> vital) {

    public static final ApertureStorage DEFAULT = new ApertureStorage(List.of(), List.of());
    public static final Codec<ApertureStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().listOf().optionalFieldOf("by_aperture", List.of())
                    .forGetter(ApertureStorage::byAperture),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("vital", List.of())
                    .forGetter(ApertureStorage::vital)
    ).apply(instance, ApertureStorage::new));

    public ApertureStorage {
        List<List<ItemStack>> kept = new ArrayList<>();
        for (int i = 0; i < Math.min(byAperture.size(), ApertureData.MAX_APERTURES); i++) {
            List<ItemStack> items = copyStacks(byAperture.get(i));
            while (!items.isEmpty() && items.getLast().isEmpty()) items.removeLast();
            kept.add(Collections.unmodifiableList(items));
        }
        while (!kept.isEmpty() && kept.getLast().isEmpty()) kept.removeLast();
        byAperture = Collections.unmodifiableList(kept);

        List<ItemStack> bound = copyStacks(
                vital.subList(0, Math.min(vital.size(), ApertureData.MAX_APERTURES)));
        while (!bound.isEmpty() && bound.getLast().isEmpty()) bound.removeLast();
        vital = Collections.unmodifiableList(bound);
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
                    || !ItemStack.isSameItemSameComponents(wanted, current)) return false;
        }
        return true;
    }

    public ApertureStorage with(int aperture, List<ItemStack> items) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> next = new ArrayList<>(byAperture);
        while (next.size() <= aperture) next.add(List.of());
        next.set(aperture, items);
        return new ApertureStorage(next, vital);
    }

    public ApertureStorage withPage(int aperture, int from, List<ItemStack> page) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES || from < 0) return this;

        List<List<ItemStack>> next = new ArrayList<>(byAperture);
        while (next.size() <= aperture) next.add(List.of());
        List<ItemStack> all = new ArrayList<>(next.get(aperture));
        while (all.size() < from + page.size()) all.add(ItemStack.EMPTY);
        for (int i = 0; i < page.size(); i++) all.set(from + i, page.get(i));
        next.set(aperture, all);
        return new ApertureStorage(next, vital);
    }

    public ApertureStorage withVital(int aperture, ItemStack stack) {
        if (aperture < 0 || aperture >= ApertureData.MAX_APERTURES) return this;

        List<ItemStack> next = new ArrayList<>(vital);
        while (next.size() <= aperture) next.add(ItemStack.EMPTY);
        next.set(aperture, stack);
        return new ApertureStorage(byAperture, next);
    }

    public ApertureStorage shiftRight() {
        if (byAperture.size() >= ApertureData.MAX_APERTURES) return this;

        List<List<ItemStack>> items = new ArrayList<>();
        items.add(List.of());
        items.addAll(byAperture);

        List<ItemStack> bound = new ArrayList<>();
        bound.add(ItemStack.EMPTY);
        for (ItemStack stack : vital) bound.add(rebound(stack));
        return new ApertureStorage(items, bound);
    }

    private static ItemStack rebound(ItemStack stack) {
        if (stack.isEmpty()) return stack;

        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.VITAL_APERTURE.get(), ApertureData.SECOND);
        return copy;
    }

    public ApertureStorage copy() { return new ApertureStorage(byAperture, vital); }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) copies.add(stack.copy());
        return copies;
    }
}
