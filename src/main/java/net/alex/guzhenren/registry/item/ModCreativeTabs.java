package net.alex.guzhenren.registry.item;

import static net.alex.guzhenren.gameplay.path.GuPath.STRENGTH;
import static net.alex.guzhenren.gameplay.path.GuPath.WOOD;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.BookletItem;
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
 * 创造模式物品栏：按物品类与道[Path]分派，或按注册链的 {@code tab}。
 *
 * <ul>
 *   <li>力道、木道蛊离开凡蛊栏进本道栏，木道蛊材仍留蛊材栏；其后凡道杀招[Killing Move]书栏，书册殿后。</li>
 *   <li>{@code tab} 取代按类选栏：元泉[Spirit Spring] BlockItem、纯水桶、钢锭、钢块借此进蛊材栏。</li>
 *   <li>⚠ 不属上述类又无 {@code tab} 的物品静默不进任何栏；进度图标有意如此。</li>
 *   <li>图标用 lambda：带 {@code tab} 的注册可能在 {@code ModItems} 初始化中触发本类，直接读字段得 null。</li>
 *   <li>语言 provider 无物品栏重载，不用这六个常量。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ModItems
 * @since 1.0.0
 */

@SuppressWarnings({ "Convert2MethodRef", "FunctionalExpressionCanBeFolded" })
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
            "strength_mortal_gu", () -> ModItems.FLOWER_BOAR_GU_1.get(), Guzhenren.id("mortal_gu"),
            item -> belongsInPathTab(item, STRENGTH));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WOOD_MORTAL_GU = register(
            "wood_mortal_gu", () -> ModItems.VITALITY_LEAF_GU_1.get(), Guzhenren.id("strength_mortal_gu"),
            item -> belongsInPathTab(item, WOOD));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KM_MORTAL = register(
            "km_mortal", () -> ModItems.KM_MORTAL_LITTLE_LIGHT_AMPLIFY.get(), Guzhenren.id("wood_mortal_gu"),
            KillingMoveBookItem.class::isInstance);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOOKLET = register(
            "booklet", () -> ModItems.GU_ATLAS_HOPE.get(), Guzhenren.id("km_mortal"),
            BookletItem.class::isInstance);

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
