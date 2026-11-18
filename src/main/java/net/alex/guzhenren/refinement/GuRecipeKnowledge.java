package net.alex.guzhenren.refinement;

import java.util.Optional;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

/**
 * 蛊方知识：玩家读蛊方物品学会的蛊方 ID 存在脑海（{@code GuRecipeKnowledgeData}），炼蛊台只认已学会的蛊方。
 * <ul>
 *   <li>双端都读已同步的附件判断 {@link #knows}；{@link #learn} 只在服务端写入，并复验配方仍在服务器上。</li>
 *   <li>研读时长按产物转数翻倍：一转 {@value #BASE_SECONDS} 秒，每高一转乘二，五转封顶；产物不是蛊（数据包蛊方）按一转
 *       算。</li>
 * </ul>
 */

public final class GuRecipeKnowledge {

    static final int BASE_SECONDS = 3;

    private GuRecipeKnowledge() {}

    public static boolean knows(@NotNull Player player, @NotNull ResourceLocation id) {
        return PlayerData.get(player).getGuRecipeKnowledge().knows(id.toString());
    }

    /** 学会指定蛊方；配方不存在或已学会时返回 {@code false}。 */
    public static boolean learn(@NotNull ServerPlayer player, @NotNull ResourceLocation id) {
        if (find(player.server.getRecipeManager(), id).isEmpty() || knows(player, id)) return false;

        PlayerData data = PlayerData.get(player);
        return data.setGuRecipeKnowledge(data.getGuRecipeKnowledge().withLearned(id.toString()));
    }

    public static Optional<GuRecipe> find(@NotNull RecipeManager recipes, @NotNull ResourceLocation id) {
        return recipes.byKey(id)
                .filter(held -> held.value().getType() == ModRecipes.REFINEMENT.get())
                .map(RecipeHolder::value)
                .filter(RefinementRecipe.class::isInstance)
                .map(recipe -> ((RefinementRecipe) recipe).gu());
    }

    public static int getStudyTicks(@NotNull GuRecipe recipe) {
        int rank = recipe.getFirst().getItem() instanceof GuItem gu ? gu.getRank().ordinal() : Rank.ONE.ordinal();
        int steps = Mth.clamp(rank, Rank.ONE.ordinal(), Rank.FIVE.ordinal()) - Rank.ONE.ordinal();
        return (BASE_SECONDS << steps) * Ticks.SECOND;
    }
}
