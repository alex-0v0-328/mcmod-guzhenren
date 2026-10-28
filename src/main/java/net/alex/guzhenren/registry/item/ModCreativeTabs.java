package net.alex.guzhenren.registry.item;

import static net.alex.guzhenren.gameplay.path.GuPath.STRENGTH;
import static net.alex.guzhenren.gameplay.path.GuPath.WOOD;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative tabs, filled by dispatching on the item's class and path.
 *
 * <p>DeferredRegister holder: four tabs ({@code mortal_gu}, {@code gu_material},
 * {@code strength_mortal_gu}, {@code wood_mortal_gu}), populated by predicates over {@link MortalGuItem},
 * {@link GuMaterialItem}, and the path: {@link GuPath#STRENGTH} and {@link GuPath#WOOD} Gu each leave the
 * mortal Gu tab for their own, while wood path Gu materials stay with the materials. An item extending neither
 * middle class lands in no tab at all -- except the two fluid items, the Spirit Spring [元泉] BlockItem and the
 * Pure Water Bucket [纯水桶], and the two metal items, the Steel Ingot [钢锭] and the Steel Block [钢块], routed into
 * the material tab explicitly. The Advancement Icon [进度图标] stays out on
 * purpose.
 *
 * <p>⚠ That miss is silent: nothing fails and nothing warns, the item simply never appears. The four
 * tab constants stay unused by the language provider (it has no creative-tab overload).
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
    public static final Supplier<CreativeModeTab> MORTAL_GU = register("mortal_gu", ModItems.HOPE_GU,
            Guzhenren.id("gu_material"), ModCreativeTabs::belongsInMortalGu);
    public static final Supplier<CreativeModeTab> GU_MATERIAL = register("gu_material", ModItems.PRIMEVAL_STONE,
            EPIC_FIGHT_ITEMS, ModCreativeTabs::belongsInGuMaterial);
    public static final Supplier<CreativeModeTab> STRENGTH_MORTAL_GU = register("strength_mortal_gu",
            ModItems.FLOWER_BOAR_GU, Guzhenren.id("mortal_gu"), item -> belongsInPathTab(item, STRENGTH));
    public static final Supplier<CreativeModeTab> WOOD_MORTAL_GU = register("wood_mortal_gu",
            ModItems.VITALITY_LEAF_GU, Guzhenren.id("strength_mortal_gu"), item -> belongsInPathTab(item, WOOD));

    private static Supplier<CreativeModeTab> register(
            String id, Supplier<? extends Item> icon, ResourceLocation before, Predicate<Item> accepted) {
        return CREATIVE_MODE_TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.guzhenren." + id))
                .icon(() -> new ItemStack(icon.get()))
                .withTabsBefore(before)
                .displayItems((parameters, output) -> accept(output, accepted))
                .build());
    }

    private static void accept(CreativeModeTab.Output output, Predicate<Item> accepted) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (accepted.test(item)) output.accept(item);
        }
    }

    static boolean belongsInMortalGu(Item item) {
        return item instanceof MortalGuItem gu && gu.getPath() != STRENGTH && gu.getPath() != WOOD;
    }

    static boolean belongsInPathTab(Item item, GuPath path) {
        return item instanceof MortalGuItem gu && gu.getPath() == path;
    }

    static boolean belongsInGuMaterial(Item item) {
        return item instanceof GuMaterialItem
                || item instanceof BlockItem blockItem && blockItem.getBlock() instanceof SpiritSpringBlock
                || item instanceof BucketItem
                || item == ModItems.STEEL_INGOT.get() || item == ModItems.STEEL_BLOCK.get();
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
