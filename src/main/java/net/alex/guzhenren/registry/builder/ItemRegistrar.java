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
 * 每物品一条声明链的登记器。
 *
 * <ul>
 *   <li>ID、工厂、两种名字、模型、标签、创造栏在注册处一并声明，datagen provider 与创造栏读回。</li>
 *   <li>仿 Create 的 Registrate 自写：{@link #item} 开 {@link ItemBuilder}，其 {@link ItemBuilder#register}
 *       把工厂交给所包 {@code DeferredRegister}、按序记 {@link Entry}；{@link #typedItem} 供保留物品类的字段。</li>
 *   <li>{@link #of} 每个 {@code DeferredRegister} 一个登记器：{@code ModItems} 经 {@code ItemRegistrar.of(ITEMS)}
 *       声明，读者同法取同批条目，无需转交，无类初始化循环。</li>
 *   <li>⚠ 读者先写声明条目、后写手写条目；既声明又手写即 bug：语言 provider 键重复抛异常，标签 provider 成员写两次致
 *       {@code runData} 漂移检查失败。模型 provider 跳过类默认形状；声明的创造栏取代类默认。</li>
 * </ul>
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
