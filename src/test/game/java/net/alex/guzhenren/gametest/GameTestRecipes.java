package net.alex.guzhenren.gametest;

import net.alex.guzhenren.refinement.GuRecipeKnowledge;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

/** 测试玩家的蛊方知识：炼蛊台只认已学会的蛊方，炼蛊类测试先学会全部。 */

final class GameTestRecipes {

    private GameTestRecipes() {}

    static void learnAll(ServerPlayer player) {
        for (RecipeHolder<RefinementRecipe> held : RefinementRecipe.getSorted(player.server.getRecipeManager())) {
            GuRecipeKnowledge.learn(player, held.id());
        }
    }
}
