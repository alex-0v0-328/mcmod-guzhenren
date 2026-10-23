package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.GuRecipe;
import net.alex.guzhenren.refinement.RefinementRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * The Gu Recipes [蛊方] the client knows, sorted once and kept: {@link RefinementRecipe#known} sorts every recipe
 * by id, which the refinement screen and its picker would otherwise pay for on every frame.
 *
 * <p>The list is rebuilt when the client level's {@link RecipeManager} is a different one, and dropped on
 * {@link RecipesUpdatedEvent} because the manager keeps its identity while a reload swaps the recipes inside it, and
 * on logout. Client thread only.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KnownRecipes {

    private KnownRecipes() {}

    private static @Nullable RecipeManager source;
    private static List<GuRecipe> cached = List.of();

    static List<GuRecipe> get() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();

        RecipeManager recipes = level.getRecipeManager();
        if (recipes != source) {
            cached = RefinementRecipe.known(recipes);
            source = recipes;
        }
        return cached;
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) { forget(); }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { forget(); }

    private static void forget() {
        source = null;
        cached = List.of();
    }
}
