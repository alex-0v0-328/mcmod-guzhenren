package net.alex.guzhenren.registry.recipe;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.RefinementRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Guzhenren.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Guzhenren.MOD_ID);
    public static final RegistryObject<RecipeType<RefinementRecipe>> REFINEMENT =
            RECIPE_TYPES.register("refinement", () -> RecipeType.simple(Guzhenren.id("refinement")));
    public static final RegistryObject<RefinementRecipe.Serializer> REFINEMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("refinement", RefinementRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
