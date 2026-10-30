package net.alex.guzhenren.registry.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * One item's registration chain, opened by {@link ItemRegistrar#item} and closed by {@link #register}.
 *
 * <p>One call per fact, like {@code GuSpec}: {@link #lang} gives the English and Simplified Chinese names, plus a
 * Traditional one only where the wording itself differs (otherwise {@code TraditionalGlyphs} converts the
 * Simplified); {@link #model} picks an {@link ItemModel} other than {@link ItemModel#FLAT}; {@link #tag} files the
 * item under item tags, each once, in call order; {@link #tab} puts it in that creative tab instead of the one its
 * class would pick. The factory is a plain {@code Supplier}, so the item's own constructor arguments, a
 * {@code GuSpec} chain among them, stay inside it untouched.
 *
 * <p>⚠ {@link #register} refuses an item without names, naming the id, before the factory reaches the
 * {@code DeferredRegister}: a declared item is never left to fall back on its raw translation key.
 *
 * @author Alex
 * @version 1.0.0
 * @see ItemRegistrar
 * @since 1.0.0
 */

public final class ItemBuilder<T extends Item> {

    private final ItemRegistrar registrar;
    private final String id;
    private final Supplier<? extends T> factory;
    private @Nullable String english;
    private @Nullable String simplified;
    private @Nullable String traditional;
    private ItemModel model = ItemModel.FLAT;
    private final Set<TagKey<Item>> tags = new LinkedHashSet<>();
    private @Nullable ResourceKey<CreativeModeTab> tab;

    ItemBuilder(ItemRegistrar registrar, String id, Supplier<? extends T> factory) {
        this.registrar = registrar;
        this.id = id;
        this.factory = factory;
    }

    public ItemBuilder<T> lang(String english, String simplified) { return lang(english, simplified, null); }

    public ItemBuilder<T> lang(String english, String simplified, @Nullable String traditional) {
        this.english = requireName(english);
        this.simplified = requireName(simplified);
        this.traditional = traditional == null ? null : requireName(traditional);
        return this;
    }

    public ItemBuilder<T> model(ItemModel model) {
        this.model = Objects.requireNonNull(model);
        return this;
    }

    @SafeVarargs
    public final ItemBuilder<T> tag(TagKey<Item>... tags) {
        this.tags.addAll(List.of(tags));
        return this;
    }

    public ItemBuilder<T> tab(DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tab) {
        this.tab = tab.getKey();
        return this;
    }

    public DeferredItem<T> register() {
        if (english == null || simplified == null) {
            throw new IllegalStateException("Item " + id + " declares no names: call lang before register");
        }
        DeferredItem<T> item = registrar.getRegister().register(id, factory);
        registrar.add(new ItemRegistrar.Entry(item, english, simplified, traditional, model, List.copyOf(tags), tab));
        return item;
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Item " + id + " has a blank name");
        return name;
    }
}
