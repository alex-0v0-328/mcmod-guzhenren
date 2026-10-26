package net.alex.guzhenren.registry.recipe;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The recipe type and serializer behind refinement [炼蛊].
 *
 * <p>DeferredRegister holder: owns the {@code refinement} {@link RecipeType} and its
 * {@link RefinementRecipe.Serializer}, wired to the mod event bus by {@code register}. No gameplay lives here.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementRecipe
 * @since 1.0.0
 */

public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Guzhenren.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Guzhenren.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<RefinementRecipe>> REFINEMENT =
            RECIPE_TYPES.register("refinement", RecipeType::simple);
    public static final DeferredHolder<RecipeSerializer<?>, RefinementRecipe.Serializer> REFINEMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("refinement", id -> new RefinementRecipe.Serializer());

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
