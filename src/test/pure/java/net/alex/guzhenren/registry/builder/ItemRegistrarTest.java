package net.alex.guzhenren.registry.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ItemRegistrar} 链的记录与拒绝：不启动，{@link UnboundItems} 的 holder 不查注册表，链可在物品注册表缺席时注册进
 * 真实 {@code DeferredRegister}；工厂从不调用；provider 是否逐字节写出原手写条目由 {@code runData} 证明。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class ItemRegistrarTest {

    private static final String NAMESPACE = "guzhenren_test";
    private static final TagKey<Item> FEED = TagKey.create(Registries.ITEM, id("feed"));
    private static final TagKey<Item> HIDDEN = TagKey.create(Registries.ITEM, id("hidden"));

    @Test
    @DisplayName("a chain records its names, model, tags and tab under the registered id, in registration order")
    void recordsTheChain() {
        ItemRegistrar registrar = ItemRegistrar.of(new UnboundItems());
        DeferredHolder<CreativeModeTab, CreativeModeTab> tab = createTab("gu_material");
        DeferredItem<Item> plain = registrar.item("plain", () -> null).lang("素").register();
        DeferredItem<Item> full = registrar.item("full", () -> null)
                .lang("满", "滿").model(ItemModel.BLOCK).tag(FEED, HIDDEN).tag(FEED).tab(tab).register();

        assertEquals(List.of(plain.getId(), full.getId()),
                registrar.getEntries().stream().map(entry -> entry.item().getId()).toList());
        ItemRegistrar.Entry entry = registrar.getEntry(full.getId());
        assertSame(full, entry.item());
        assertEquals("满", entry.simplified());
        assertEquals("滿", entry.traditional());
        assertEquals(ItemModel.BLOCK, entry.model());
        assertEquals(List.of(FEED, HIDDEN), entry.tags());
        assertEquals(tab.getKey(), entry.tab());
    }

    @Test
    @DisplayName("an unadorned chain is a flat model in no tag and no tab, its Traditional name converted")
    void defaultsStayOut() {
        ItemRegistrar registrar = ItemRegistrar.of(new UnboundItems());
        DeferredItem<Item> plain = registrar.item("plain", () -> null).lang("素").register();

        ItemRegistrar.Entry entry = registrar.getEntry(plain.getId());
        assertEquals(ItemModel.FLAT, entry.model());
        assertTrue(entry.tags().isEmpty());
        assertNull(entry.tab());
        assertNull(entry.traditional());
        assertNull(registrar.getEntry(id("undeclared")));
    }

    @Test
    @DisplayName("an item without names is refused, by id, before it reaches the DeferredRegister")
    void refusesANamelessItem() {
        UnboundItems items = new UnboundItems();
        ItemRegistrar registrar = ItemRegistrar.of(items);

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> registrar.item("nameless", () -> null).register());
        assertTrue(refused.getMessage().contains("nameless"), refused.getMessage());
        assertTrue(items.getEntries().isEmpty());
        assertTrue(registrar.getEntries().isEmpty());
    }

    @Test
    @DisplayName("a blank name is refused, naming the item")
    void refusesABlankName() {
        ItemRegistrar registrar = ItemRegistrar.of(new UnboundItems());

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> registrar.item("blank", () -> null).lang(" "));
        assertTrue(refused.getMessage().contains("blank"), refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> registrar.item("blank", () -> null).lang("空", ""));
    }

    @Test
    @DisplayName("one registrar per DeferredRegister: the same one back for the same register, a fresh one otherwise")
    void keepsOneRegistrarPerRegister() {
        UnboundItems items = new UnboundItems();

        assertSame(ItemRegistrar.of(items), ItemRegistrar.of(items));
        assertNotSame(ItemRegistrar.of(items), ItemRegistrar.of(new UnboundItems()));
    }

    @Test
    @DisplayName("a repeated id fails in the DeferredRegister and leaves the first entry as it was")
    void keepsTheFirstOfARepeatedId() {
        ItemRegistrar registrar = ItemRegistrar.of(new UnboundItems());
        DeferredItem<Item> first = registrar.item("twice", () -> null).lang("一").register();

        assertThrows(IllegalArgumentException.class,
                () -> registrar.item("twice", () -> null).lang("二").register());
        assertEquals("一", registrar.getEntry(first.getId()).simplified());
        assertEquals(1, registrar.getEntries().size());
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path); }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> createTab(String path) {
        return new DeferredHolder<>(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id(path))) {
            @Override
            protected Registry<CreativeModeTab> getRegistry() { return null; }
        };
    }

    private static final class UnboundItems extends DeferredRegister.Items {

        UnboundItems() { super(NAMESPACE); }

        @Override
        protected <I extends Item> @NotNull DeferredItem<I> createHolder(
                @NotNull ResourceKey<? extends Registry<Item>> registryKey, @NotNull ResourceLocation key) {
            return new DeferredItem<>(ResourceKey.create(Registries.ITEM, key)) {
                @Override
                protected Registry<Item> getRegistry() { return null; }
            };
        }
    }
}
