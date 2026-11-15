package net.alex.guzhenren.registry.item;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModCreativeTabsGameTests {

    private ModCreativeTabsGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void mortalGuTabExcludesStrength(GameTestHelper helper) {
        assertFalse(ModCreativeTabs.belongsInMortalGu(ModItems.WHITE_BOAR_GU_1.get()));
        assertTrue(ModCreativeTabs.belongsInMortalGu(ModItems.HOPE_GU.get()));
        assertFalse(ModCreativeTabs.belongsInMortalGu(ModItems.PRIMEVAL_STONE.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void woodGuHaveTheirOwnTab(GameTestHelper helper) {
        assertFalse(ModCreativeTabs.belongsInMortalGu(ModItems.VITALITY_LEAF_GU_1.get()));
        assertFalse(ModCreativeTabs.belongsInMortalGu(ModItems.NINE_LEAF_VITALITY_GRASS_3.get()));
        assertTrue(ModCreativeTabs.belongsInPathTab(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU_3.get(), GuPath.WOOD));
        assertTrue(ModCreativeTabs.belongsInPathTab(ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), GuPath.WOOD));
        assertFalse(ModCreativeTabs.belongsInPathTab(ModItems.MOON_ORCHID_PETALS.get(), GuPath.WOOD));
        assertTrue(ModCreativeTabs.belongsInGuMaterial(ModItems.MOON_ORCHID_PETALS.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void advancementIconLandsInNoTab(GameTestHelper helper) {
        assertFalse(ModCreativeTabs.belongsInMortalGu(ModItems.ADVANCEMENT_ICON.get()));
        assertFalse(ModCreativeTabs.belongsInGuMaterial(ModItems.ADVANCEMENT_ICON.get()));
        for (GuPath path : GuPath.values()) {
            assertFalse(ModCreativeTabs.belongsInPathTab(ModItems.ADVANCEMENT_ICON.get(), path));
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void materialTabShowsTheFluidAndMetalItems(GameTestHelper helper) {
        Set<Item> material = getShown(helper, ModCreativeTabs.GU_MATERIAL.get());
        Set<Item> mortal = getShown(helper, ModCreativeTabs.MORTAL_GU.get());
        for (Item item : List.of(ModItems.SPIRIT_SPRING.get(), ModItems.PURE_WATER_BUCKET.get(),
                ModItems.STEEL_INGOT.get(), ModItems.STEEL_BLOCK.get(), ModItems.PRIMEVAL_STONE.get())) {
            assertTrue(material.contains(item), () -> item + " missing from the material tab");
            assertFalse(mortal.contains(item), item + " shown in the mortal Gu tab");
        }
        assertTrue(mortal.contains(ModItems.HOPE_GU.get()), "Hope Gu missing from the mortal Gu tab");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void everyItemShowsInOneTabAtMost(GameTestHelper helper) {
        List<Set<Item>> tabs = List.of(getShown(helper, ModCreativeTabs.MORTAL_GU.get()),
                getShown(helper, ModCreativeTabs.GU_MATERIAL.get()),
                getShown(helper, ModCreativeTabs.STRENGTH_MORTAL_GU.get()),
                getShown(helper, ModCreativeTabs.WOOD_MORTAL_GU.get()));
        for (var entry : ModItems.ITEMS.getEntries()) {
            long shownIn = tabs.stream().filter(tab -> tab.contains(entry.get())).count();
            assertTrue(shownIn <= 1, () -> entry.getId() + " shown in " + shownIn + " tabs");
        }
        assertTrue(tabs.stream().noneMatch(tab -> tab.contains(ModItems.ADVANCEMENT_ICON.get())),
                "Advancement Icon shown in a tab");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void declaredItemsShowInTheirOwnTab(GameTestHelper helper) {
        for (ItemRegistrar.Entry entry : ItemRegistrar.of(ModItems.ITEMS).getEntries()) {
            if (entry.tab() == null) continue;
            Set<Item> shown = getShown(helper, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(entry.tab()));
            assertTrue(shown.contains(entry.item().get()),
                    () -> entry.item().getId() + " missing from its declared tab " + entry.tab().location());
        }
        helper.succeed();
    }

    private static Set<Item> getShown(GameTestHelper helper, CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        return tab.getDisplayItems().stream().map(ItemStack::getItem).collect(Collectors.toSet());
    }
}
