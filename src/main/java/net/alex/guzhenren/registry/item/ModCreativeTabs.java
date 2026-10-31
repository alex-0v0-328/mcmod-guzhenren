package net.alex.guzhenren.registry.item;

import static net.alex.guzhenren.gameplay.path.GuPath.STRENGTH;
import static net.alex.guzhenren.gameplay.path.GuPath.WOOD;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative tabs, filled by dispatching on the item's class and path, or on the tab its registration declares.
 *
 * <p>DeferredRegister holder: five tabs ({@code mortal_gu}, {@code gu_material},
 * {@code strength_mortal_gu}, {@code wood_mortal_gu}, {@code km_mortal}), populated by predicates over
 * {@link MortalGuItem}, {@link GuMaterialItem}, {@link KillingMoveBookItem} and the path: {@link GuPath#STRENGTH} and
 * {@link GuPath#WOOD} Gu each leave the mortal Gu tab for their own, while wood path Gu materials stay with the
 * materials, and the mortal killing move books [凡道杀招] get the last tab. An item extending none of those classes
 * lands in no tab at all, unless its {@link ItemRegistrar} chain names a {@code tab}: the two fluid
 * items, the Spirit Spring [元泉] BlockItem and the Pure Water Bucket [纯水桶], and the two metal items, the Steel
 * Ingot [钢锭] and the Steel Block [钢块], name the material tab that way. The Advancement Icon [进度图标] stays out
 * on purpose.
 *
 * <p>⚠ That miss is silent: nothing fails and nothing warns, the item simply never appears. The five
 * tab constants stay unused by the language provider (it has no creative-tab overload).
 *
 * <p>A chain's {@code tab} sends the item to that tab alone, in place of the one its class would pick. The icons
 * are lambdas, never the {@code ModItems} fields themselves: a
 * registration naming its tab can initialize this class while {@code ModItems} is still initializing, and reading a
 * field then would capture a null icon.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModItems
 * @since 1.0.0
 */

public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Guzhenren.MOD_ID);
    private static final ResourceLocation EPIC_FIGHT_ITEMS =
            ResourceLocation.fromNamespaceAndPath("epicfight", "items");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MORTAL_GU = register("mortal_gu",
            () -> ModItems.HOPE_GU.get(), Guzhenren.id("gu_material"), ModCreativeTabs::belongsInMortalGu);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GU_MATERIAL = register("gu_material",
            () -> ModItems.PRIMEVAL_STONE.get(), EPIC_FIGHT_ITEMS, ModCreativeTabs::belongsInGuMaterial);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STRENGTH_MORTAL_GU = register(
            "strength_mortal_gu", () -> ModItems.FLOWER_BOAR_GU.get(), Guzhenren.id("mortal_gu"),
            item -> belongsInPathTab(item, STRENGTH));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WOOD_MORTAL_GU = register(
            "wood_mortal_gu", () -> ModItems.VITALITY_LEAF_GU.get(), Guzhenren.id("strength_mortal_gu"),
            item -> belongsInPathTab(item, WOOD));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KM_MORTAL = register(
            "km_mortal", () -> ModItems.KM_MORTAL_LITTLE_LIGHT_AMPLIFY.get(), Guzhenren.id("wood_mortal_gu"),
            KillingMoveBookItem.class::isInstance);

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(
            String id, Supplier<? extends Item> icon, ResourceLocation before, Predicate<Item> accepted) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Guzhenren.id(id));
        return CREATIVE_MODE_TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.guzhenren." + id))
                .icon(() -> new ItemStack(icon.get()))
                .withTabsBefore(before)
                .displayItems((parameters, output) -> accept(output, tab, accepted))
                .build());
    }

    private static void accept(CreativeModeTab.Output output, ResourceKey<CreativeModeTab> tab,
                               Predicate<Item> accepted) {
        ItemRegistrar registrar = ItemRegistrar.of(ModItems.ITEMS);
        for (var entry : ModItems.ITEMS.getEntries()) {
            ItemRegistrar.Entry declared = registrar.getEntry(entry.getId());
            ResourceKey<CreativeModeTab> declaredTab = declared == null ? null : declared.tab();
            Item item = entry.get();
            if (declaredTab != null ? declaredTab.equals(tab) : accepted.test(item)) output.accept(item);
        }
    }

    static boolean belongsInMortalGu(Item item) {
        return item instanceof MortalGuItem gu && gu.getPath() != STRENGTH && gu.getPath() != WOOD;
    }

    static boolean belongsInPathTab(Item item, GuPath path) {
        return item instanceof MortalGuItem gu && gu.getPath() == path;
    }

    static boolean belongsInGuMaterial(Item item) {
        return item instanceof GuMaterialItem;
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
