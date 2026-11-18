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
 * 单个物品的链式注册构造器，由 {@link ItemRegistrar#item} 或 {@link ItemRegistrar#typedItem} 开启并以 {@link #register}
 * 收尾。
 * <ul>
 *   <li>采用一事一调用链式风格（与 {@code GuProperties} 一致）：{@link #lang} 声明英文与简体中文译名（繁体仅在用词与简
 *       中不同时显式传入，否则由 {@code TraditionalGlyphs} 自动转换），{@link #model} 选择 {@link ItemModel#FLAT} 之外
 *       的模型形状，{@link #tag} 按顺序追加物品标签，{@link #tab} 指定归属创造模式物品栏以覆盖按类默认规则。</li>
 *   <li>物品构造工厂保持为普通 {@code Supplier}，物品自身的参数链（如 {@code GuProperties}）原样保留在工厂内部。</li>
 *   <li>⚠ {@link #register} 会在提交至 {@code DeferredRegister} 前强校验中英文名称非空并携带 ID 报错，防止缺失译名的物
 *       品回退到原始翻译键。</li>
 * </ul>
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
