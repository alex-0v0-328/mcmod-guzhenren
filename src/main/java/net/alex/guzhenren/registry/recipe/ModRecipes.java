package net.alex.guzhenren.registry.recipe;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.armor.SchoolUniformDyeRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 炼蛊配方类型 {@code refinement}（{@link RefinementRecipe}）及其序列化器的 {@code DeferredRegister} 注册容器，本身不包
 * 含玩法逻辑。
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
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SchoolUniformDyeRecipe>>
            SCHOOL_UNIFORM_DYE_SERIALIZER = RECIPE_SERIALIZERS.register("school_uniform_dye",
            () -> new SimpleCraftingRecipeSerializer<>(SchoolUniformDyeRecipe::new));

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
