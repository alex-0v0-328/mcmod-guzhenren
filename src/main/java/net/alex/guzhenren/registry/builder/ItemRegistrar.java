package net.alex.guzhenren.registry.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * One chain per item: the id, the factory, both names, the model kind, the tags and the creative tab, declared
 * together where the item registers, then read back by the datagen providers and the creative tabs.
 *
 * <p>Modeled on Create's Registrate, built in-house: {@link #item} opens an {@link ItemBuilder}, whose
 * {@link ItemBuilder#register} hands the factory to the wrapped {@code DeferredRegister} and records an
 * {@link Entry} in registration order. {@link #typedItem} is the same for a field that keeps its item class.
 * {@link #of} keeps one registrar per {@code DeferredRegister}, so {@code ModItems} declares through
 * {@code ItemRegistrar.of(ITEMS)} and every reader reaches the same entries through the same call, without
 * {@code ModItems} handing them over and without a class-init cycle between the two.
 *
 * <p>⚠ A reader writes the declared entries first and its hand-written ones after, and an item both declared and
 * written by hand is a bug: the language provider throws on the repeated key, the tag provider writes the member
 * twice and fails the {@code runData} drift check. The model provider skips the shape a declared item's class would
 * pick, and a declared tab replaces the one its class would pick; without a tab the class still decides.
 *
 * @author Alex
 * @version 1.0.0
 * @see ItemBuilder
 * @since 1.0.0
 */

public final class ItemRegistrar {

    private static final Map<DeferredRegister.Items, ItemRegistrar> REGISTRARS = new ConcurrentHashMap<>();
    private final DeferredRegister.Items register;
    private final Map<ResourceLocation, Entry> entries = new LinkedHashMap<>();

    private ItemRegistrar(DeferredRegister.Items register) { this.register = register; }

    public static ItemRegistrar of(DeferredRegister.Items register) {
        return REGISTRARS.computeIfAbsent(register, ItemRegistrar::new);
    }

    public ItemBuilder<Item> item(String id, Supplier<? extends Item> factory) {
        return new ItemBuilder<>(this, id, factory);
    }

    public <T extends Item> ItemBuilder<T> typedItem(String id, Supplier<T> factory) {
        return new ItemBuilder<>(this, id, factory);
    }

    public List<Entry> getEntries() { return List.copyOf(entries.values()); }

    public @Nullable Entry getEntry(ResourceLocation id) { return entries.get(id); }

    DeferredRegister.Items getRegister() { return register; }

    void add(Entry entry) { entries.put(entry.item().getId(), entry); }

    public record Entry(DeferredItem<? extends Item> item, String english, String simplified,
                        @Nullable String traditional, ItemModel model, List<TagKey<Item>> tags,
                        @Nullable ResourceKey<CreativeModeTab> tab) {}
}
