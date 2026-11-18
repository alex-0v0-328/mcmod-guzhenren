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
 * 每物品一条声明链的集中登记器，在注册点一并声明 ID、工厂、中英文名称、模型形状、标签与创造栏并供 datagen provider 与创
 * 造栏遍历读回。
 * <ul>
 *   <li>仿照 Registrate 模式自建：{@link #item} 与 {@link #typedItem} 创建 {@link ItemBuilder}，由
 *       {@link ItemBuilder#register} 将工厂注册到包装的 {@code DeferredRegister.Items} 并按注册顺序写入 {@link Entry}
 *       列表。</li>
 *   <li>{@link #of} 为每个 {@code DeferredRegister.Items} 维护唯一实例，使 {@code ModItems} 与下游读取方无需额外转交引
 *       用且规避类初始化循环。</li>
 *   <li>⚠ 所有下游 provider 与创造栏均按「先读登记条目、再读手写补充」处理，同一物品绝不可既走登记链又在手写段重复声明
 *       （否则语言 provider 会因重复键抛异常，标签 provider 会因重复成员导致 {@code runData} 漂移检查失败）；物品栏若在
 *       此显式声明则会覆盖按类默认分派，模型 provider 亦会跳过已登记项的默认形状。</li>
 * </ul>
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
