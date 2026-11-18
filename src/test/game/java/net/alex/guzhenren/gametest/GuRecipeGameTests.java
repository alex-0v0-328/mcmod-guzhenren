package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.alex.guzhenren.refinement.GuRecipeKnowledge;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 蛊方[Gu Recipe]：炼蛊台只认学会的蛊方；长按研读满时长学会，书不消耗；研读时长按产物转数 3 / 6 / 12 / 24 / 48 秒；
 * 学会后按下不再研读；未开保留物品栏死亡才遗忘；每张书文都对着服务器上的一张蛊方；每张蛊方的材料都放得进它的格（外圈
 * 收非蛊，内圈只收凡蛊）。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuRecipeGameTests {

    private static final ResourceLocation HABITUAL = Guzhenren.id("refinement/habitual_strength_gu_4");
    private static final ResourceLocation WHITE_JADE = Guzhenren.id("refinement/white_jade_gu_2");

    private GuRecipeGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void anUnlearnedRecipeCannotBeSelected(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        RefinementMenu menu = new RefinementMenu(1, player.getInventory());
        int index = indexOf(helper, HABITUAL);

        assertFalse(menu.clickMenuButton(player, RefinementMenu.BUTTON_RECIPE_BASE + index));
        assertEquals(-1, menu.getSelected());

        GuRecipeKnowledge.learn(player, HABITUAL);
        assertTrue(menu.clickMenuButton(player, RefinementMenu.BUTTON_RECIPE_BASE + index));
        assertEquals(index, menu.getSelected());
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void studyingToTheEndLearnsAndKeepsTheBook(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ItemStack book = GuRecipeItem.of(ModItems.GU_RECIPE.get(), HABITUAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);

        book.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "an unlearned recipe did not start studying");
        assertEquals(24 * 20, book.getUseDuration(player));

        ItemStack left = book.finishUsingItem(helper.getLevel(), player);
        assertTrue(GuRecipeKnowledge.knows(player, HABITUAL), "studying to the end did not learn the recipe");
        assertEquals(1, left.getCount());
        assertTrue(left.is(ModItems.GU_RECIPE.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void studyTimeDoublesWithTheProductsRank(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        assertEquals(6 * 20, GuRecipeItem.of(ModItems.GU_RECIPE.get(), WHITE_JADE).getUseDuration(player));
        assertEquals(24 * 20, GuRecipeItem.of(ModItems.GU_RECIPE.get(), HABITUAL).getUseDuration(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aLearnedRecipeOpensWithoutStudying(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        GuRecipeKnowledge.learn(player, HABITUAL);
        ItemStack book = GuRecipeItem.of(ModItems.GU_RECIPE.get(), HABITUAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);

        book.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertFalse(player.isUsingItem(), "a learned recipe started studying again");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyADeathWithoutKeepInventoryForgetsRecipes(GameTestHelper helper) {
        ServerPlayer from = mortal(helper);
        ServerPlayer to = mortal(helper);
        GuRecipeKnowledge.learn(to, HABITUAL);

        PlayerDataService.onClone(from, to, true, true);
        assertTrue(GuRecipeKnowledge.knows(to, HABITUAL));

        PlayerDataService.onClone(from, to, true, false);
        assertFalse(GuRecipeKnowledge.knows(to, HABITUAL));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void everyRecipeTextNamesARecipeOnTheServer(GameTestHelper helper) {
        for (Booklet booklet : Booklet.values()) {
            booklet.getRecipe().ifPresent(id -> assertTrue(
                    GuRecipeKnowledge.find(helper.getLevel().getServer().getRecipeManager(), id).isPresent(),
                    booklet + " names " + id + ", no recipe"));
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void everyIngredientFitsTheCellsItSitsIn(GameTestHelper helper) {
        for (RecipeHolder<RefinementRecipe> held : RefinementRecipe.getSorted(
                helper.getLevel().getServer().getRecipeManager())) {
            GuRecipe recipe = held.value().gu();
            for (int n = 0; n < recipe.ingredients().size(); n++) {
                for (ItemStack option : recipe.ingredients().get(n).ingredient().getItems()) {
                    boolean gu = option.getItem() instanceof MortalGuItem;
                    for (int cell : recipe.slots().get(n)) {
                        assertTrue(gu == cell >= GuRecipe.RING_SIZE, held.id() + " puts " + option.getItem()
                                + " in cell " + cell + ", where the grid refuses it");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static int indexOf(GameTestHelper helper, ResourceLocation id) {
        List<RecipeHolder<RefinementRecipe>> all = RefinementRecipe.getSorted(
                helper.getLevel().getServer().getRecipeManager());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(id)) return i;
        }
        throw new IllegalStateException(id + " missing");
    }
}
