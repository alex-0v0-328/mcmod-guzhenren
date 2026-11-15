package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.item.ModItemTags;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 玉、翡翠及其原石（玉原石、翡翠原石）：加载的配方在熔炉与高炉以原版粗矿时长把一块原石烧成一块成品；加载的食物标签使
 * 玉、翡翠成为玉皮蛊[Jade Skin Gu]与白玉蛊[White Jade Gu]的唯一食物。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JadeGameTests {

    private JadeGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void roughJadeSmeltsIntoJadeInAFurnaceAndABlastFurnace(GameTestHelper helper) {
        assertCooks(helper, ModItems.ROUGH_JADE.get(), ModItems.JADE.get());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void roughJadeiteSmeltsIntoJadeiteInAFurnaceAndABlastFurnace(GameTestHelper helper) {
        assertCooks(helper, ModItems.ROUGH_JADEITE.get(), ModItems.JADEITE.get());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void jadeAndJadeiteAreTheOnlyFoodOfBothJadeSkinGu(GameTestHelper helper) {
        for (Item food : new Item[] { ModItems.JADE.get(), ModItems.JADEITE.get() }) {
            ItemStack stack = new ItemStack(food);
            assertTrue(stack.is(ModItemTags.JADE_SKIN_FEED), () -> food + " feeds the Jade Skin Gu");
            assertTrue(stack.is(ModItemTags.WHITE_JADE_FEED), () -> food + " feeds the White Jade Gu");
        }
        for (Item raw : new Item[] { ModItems.ROUGH_JADE.get(), ModItems.ROUGH_JADEITE.get() }) {
            assertFalse(new ItemStack(raw).is(ModItemTags.JADE_SKIN_FEED), raw + " is uncooked");
            assertFalse(new ItemStack(raw).is(ModItemTags.WHITE_JADE_FEED), raw + " is uncooked");
        }
        assertFalse(new ItemStack(Items.EMERALD).is(ModItemTags.JADE_SKIN_FEED), "Emerald no longer feeds it");
        assertFalse(new ItemStack(Items.QUARTZ).is(ModItemTags.WHITE_JADE_FEED), "Quartz no longer feeds it");
        helper.succeed();
    }

    private static void assertCooks(GameTestHelper helper, Item raw, Item cut) {
        AbstractCookingRecipe smelting = cook(helper, RecipeType.SMELTING, raw, cut);
        AbstractCookingRecipe blasting = cook(helper, RecipeType.BLASTING, raw, cut);

        assertEquals(10 * Ticks.SECOND, smelting.getCookingTime(), "furnace ticks");
        assertEquals(5 * Ticks.SECOND, blasting.getCookingTime(), "blast furnace ticks");
        assertEquals(0.7, smelting.getExperience(), 1e-6);
        assertEquals(0.7, blasting.getExperience(), 1e-6);
    }

    private static AbstractCookingRecipe cook(GameTestHelper helper, RecipeType<? extends AbstractCookingRecipe> type,
                                              Item raw, Item cut) {
        ServerLevel level = helper.getLevel();
        AbstractCookingRecipe recipe = level.getRecipeManager()
                .getRecipeFor(type, new SingleRecipeInput(new ItemStack(raw)), level)
                .orElseThrow(() -> new AssertionError(type + " has no " + raw + " recipe"))
                .value();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        assertTrue(result.is(cut), () -> type + " gives " + result);
        assertEquals(1, result.getCount(), type + " count");
        return recipe;
    }
}
