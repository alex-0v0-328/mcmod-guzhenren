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
import net.alex.guzhenren.refinement.GuRecipeItem;
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
 * 本模组创造模式物品栏注册表，按物品类型、所属流派或在 {@link ItemRegistrar} 中显式声明的 {@code tab} 规则分配物品。
 * <ul>
 *   <li>栏位顺序依次为蛊材、凡蛊（排除独立建栏的力道与木道凡蛊）、力道蛊虫、木道蛊虫、书（蛊方、杀招[Killing Move]书、
 *       图册与史书合一页）；蛊材排在 {@code OTHER_MOD_TABS} 列出的整合包模组页签之后，使第二页上排为四个外部页签加蛊
 *       材、下排为本模组其余四页；未安装的模组 ID 被 NeoForge 排序忽略。</li>
 *   <li>谓词按传入顺序分轮列出：书页先列蛊方（按客户端配方表每张列一本），再列杀招书，最后列书册（图册在前、史书按
 *       节在后，即注册顺序）；同一轮内按注册顺序，显式声明 {@code tab} 的物品只在第一轮列出。</li>
 *   <li>通过注册链 {@code tab} 参数声明的物品（如元泉[Spirit Spring]
 *       {@code BlockItem}、纯水桶、钢锭、钢块、知心草种子与黑土耕地）直接归入指定栏位并覆盖按类默认规则。</li>
 *   <li>⚠ 既不符合任意栏位谓词又未显式指定 {@code tab} 的物品会静默跳过而不进入任何创造栏（进度图标物品即有意如
 *       此）。</li>
 *   <li>⚠ 图标必须使用 lambda 延迟求值（含 {@code tab} 的物品注册可能在 {@code ModItems} 类初始化中途触发本类加载，直接
 *       读静态字段会得到 {@code null}）；本类五个栏位常量均不参与语言 datagen。</li>
 * </ul>
 */

@SuppressWarnings({ "Convert2MethodRef", "FunctionalExpressionCanBeFolded" })
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Guzhenren.MOD_ID);
    private static final ResourceLocation[] OTHER_MOD_TABS = {
            ResourceLocation.fromNamespaceAndPath("epicfight", "items"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "farmersdelight"),
            ResourceLocation.fromNamespaceAndPath("ftblibrary", "ftb_library"),
            ResourceLocation.fromNamespaceAndPath("ldlib2", "ldlib2_dev_tab")
    };
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MORTAL_GU = register("mortal_gu",
            () -> ModItems.HOPE_GU.get(), Guzhenren.id("gu_material"), ModCreativeTabs::belongsInMortalGu);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GU_MATERIAL = register("gu_material",
            () -> ModItems.PRIMEVAL_STONE.get(), OTHER_MOD_TABS, ModCreativeTabs::belongsInGuMaterial);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STRENGTH_MORTAL_GU = register(
            "strength_mortal_gu", () -> ModItems.FLOWER_BOAR_GU_1.get(), Guzhenren.id("mortal_gu"),
            item -> belongsInPathTab(item, STRENGTH));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WOOD_MORTAL_GU = register(
            "wood_mortal_gu", () -> ModItems.VITALITY_LEAF_GU_1.get(), Guzhenren.id("strength_mortal_gu"),
            item -> belongsInPathTab(item, WOOD));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOOKS = register(
            "books", () -> ModItems.GU_RECIPE.get(), new ResourceLocation[] { Guzhenren.id("wood_mortal_gu") },
            GuRecipeItem.class::isInstance, KillingMoveBookItem.class::isInstance, BookletItem.class::isInstance);

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(
            String id, Supplier<? extends Item> icon, ResourceLocation before, Predicate<Item> accepted) {
        return register(id, icon, new ResourceLocation[] { before }, accepted);
    }

    @SafeVarargs
    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(
            String id, Supplier<? extends Item> icon, ResourceLocation[] before, Predicate<Item>... rounds) {
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Guzhenren.id(id));
        return CREATIVE_MODE_TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.guzhenren." + id))
                .icon(() -> new ItemStack(icon.get()))
                .withTabsBefore(before)
                .displayItems((parameters, output) -> {
                    for (int round = 0; round < rounds.length; round++) {
                        accept(output, tab, rounds[round], round == 0);
                    }
                })
                .build());
    }

    private static void accept(CreativeModeTab.Output output, ResourceKey<CreativeModeTab> tab,
                               Predicate<Item> accepted, boolean firstRound) {
        ItemRegistrar registrar = ItemRegistrar.of(ModItems.ITEMS);
        for (var entry : ModItems.ITEMS.getEntries()) {
            ItemRegistrar.Entry declared = registrar.getEntry(entry.getId());
            ResourceKey<CreativeModeTab> declaredTab = declared == null ? null : declared.tab();
            Item item = entry.get();
            if (declaredTab != null ? !firstRound || !declaredTab.equals(tab) : !accepted.test(item)) continue;

            if (item instanceof GuRecipeItem) {
                for (ResourceLocation id : GuRecipeItem.listForCreativeTab()) output.accept(GuRecipeItem.of(item, id));
            } else {
                output.accept(item);
            }
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
