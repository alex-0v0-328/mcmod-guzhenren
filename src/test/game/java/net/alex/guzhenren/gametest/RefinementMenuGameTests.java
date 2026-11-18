package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;

/**
 * 炼蛊界面测试。自动填入不拿他人本命蛊；无论界面广播几次，炼制计时每 tick 只进一次。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RefinementMenuGameTests {

    private RefinementMenuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void refinementAutofillSkipsAnotherPlayersVital(GameTestHelper helper) {
        ServerPlayer owner = awakenedMortal(helper);
        ServerPlayer holder = awakenedMortal(helper);
        ItemStack foreign = GameTestGu.refined(ModItems.LIQUOR_WORM_1.get(), 3);
        VitalGuState.bind(foreign, owner, ApertureData.PRIMARY);
        holder.getInventory().setItem(9, foreign);
        GameTestRecipes.learnAll(holder);
        RefinementMenu menu = new RefinementMenu(1, holder.getInventory());

        menu.clickMenuButton(holder, RefinementMenu.BUTTON_RECIPE_BASE + fourFlavorsIndex(helper));

        helper.assertTrue(VitalGuState.isVital(holder.getInventory().getItem(9)), "autofill took another player's vital");
        for (int i = 0; i < GuRecipe.INPUT_SIZE; i++) {
            helper.assertTrue(!VitalGuState.isVital(menu.getSlot(i).getItem()), "a foreign vital sits in the grid");
        }
        menu.removed(holder);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refinementClockAdvancesOncePerTick(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureEssenceService.refill(player);
        List<Item> ingredients = List.of(ModItems.SOUR_LIQUOR.get(), ModItems.BITTER_LIQUOR.get(),
                ModItems.SPICY_LIQUOR.get(), ModItems.SWEET_LIQUOR.get(), ModItems.LIQUOR_WORM_1.get(),
                ModItems.LIQUOR_WORM_1.get());
        for (int i = 0; i < ingredients.size(); i++) player.getInventory().setItem(9 + i, new ItemStack(ingredients.get(i)));
        GameTestRecipes.learnAll(player);
        RefinementMenu menu = new RefinementMenu(1, player.getInventory());
        menu.clickMenuButton(player, RefinementMenu.BUTTON_RECIPE_BASE + fourFlavorsIndex(helper));
        menu.clickMenuButton(player, RefinementMenu.BUTTON_CRAFT);
        helper.assertTrue(menu.isRunning(), "the ritual did not start");

        int before = menu.getPhaseLeft();
        menu.broadcastChanges();
        menu.broadcastChanges();
        menu.broadcastChanges();

        helper.assertTrue(before - menu.getPhaseLeft() <= 1, "three broadcasts in one tick advanced the ritual "
                + (before - menu.getPhaseLeft()) + " ticks");
        menu.removed(player);
        helper.succeed();
    }

    private static int fourFlavorsIndex(GameTestHelper helper) {
        List<GuRecipe> known = RefinementRecipe.getKnown(helper.getLevel().getServer().getRecipeManager());
        for (int i = 0; i < known.size(); i++) {
            if (known.get(i).getFirst().is(ModItems.FOUR_FLAVORS_LIQUOR_WORM_2.get())) return i;
        }
        throw new IllegalStateException("four flavors liquor worm recipe missing");
    }
}
