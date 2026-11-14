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
 * 单个物品的注册链：{@link ItemRegistrar#item} 开启，{@link #register} 收尾。
 *
 * <ul>
 *   <li>一事一调用，同 {@code GuProperties}：{@link #lang} 英文、简中名，繁体仅措辞不同时给，否则
 *       {@code TraditionalGlyphs} 转换；{@link #model} 选 {@link ItemModel#FLAT} 外的模型；{@link #tag}
 *       按序归入物品标签，各一次；{@link #tab} 指定创造栏，取代类默认。</li>
 *   <li>工厂是普通 {@code Supplier}，物品构造参数（含 {@code GuProperties} 链）原样留在其中。</li>
 *   <li>⚠ {@link #register} 在工厂入 {@code DeferredRegister} 前拒收无名物品并报 ID，免得落回原始翻译键。</li>
 * </ul>
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
