package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端已知的蛊方，排序一次后缓存。
 * <ul>
 *   <li>{@link RefinementRecipe#getKnown} 按 id 排序全部蛊方，不缓存则炼蛊界面和蛊方选择器每帧都付这笔开销。</li>
 *   <li>客户端 {@link RecipeManager} 换实例时重建；{@link RecipesUpdatedEvent}（重载换配方不换实例）和登出时丢弃。</li>
 *   <li>只在客户端线程用；两个可变静态字段是客户端唯一的蛊方缓存及其来源管理器，重载或登出时清空，下次读取再填。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KnownRecipes {

    private KnownRecipes() {}

    private static @Nullable RecipeManager source;
    private static List<GuRecipe> cached = List.of();

    static List<GuRecipe> get() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();

        RecipeManager recipes = level.getRecipeManager();
        if (recipes != source) {
            cached = RefinementRecipe.getKnown(recipes);
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
