package net.alex.guzhenren.datagen.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组配方。
 *
 * <ul>
 *   <li>蛊方各画成网格 ASCII 图并逐字转录（{@code '.'} 空格，{@code ' '} 切角）；{@code key} 字母所填每格各为一份配料，
 *       {@code shared} 字母是同一份配料，可放在画了它的任一格。</li>
 *   <li>⚠ 角画错、行太短、字母没配料都让数据生成失败；图样布局与其中每个数字都由 Alex 定。</li>
 *   <li>熔炉、高炉把一种蛊材烧成另一种；钢锭只走高炉，另有钢块：钢是铁再烧一次，普通熔炉不行
 *       （{@link #addBlasting}）。</li>
 *   <li>炼蛊输出在 {@code data/guzhenren/recipe/refinement/}，烧炼与存储块直接在 {@code data/guzhenren/recipe/}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public class ModRecipeProvider extends RecipeProvider {

    private static final String FOLDER = "refinement/";
    private static final char EMPTY = '.';
    private static final char OUTSIDE = ' ';

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        addRefinement(output, ModItems.FOUR_FLAVORS_LIQUOR_WORM_2, 1L, 1L, 50, List.of(16, 16, 16, 16),
                Map.of('s', GuIngredient.of(ModItems.SOUR_LIQUOR.get(), 1),
                        'b', GuIngredient.of(ModItems.BITTER_LIQUOR.get(), 1),
                        'h', GuIngredient.of(ModItems.SPICY_LIQUOR.get(), 1),
                        'w', GuIngredient.of(ModItems.SWEET_LIQUOR.get(), 1),
                        'g', GuIngredient.of(ModItems.LIQUOR_WORM_1.get(), 1)),
                " .s. ",
                ".....",
                "bg.gh",
                ".....",
                " .w. ");

        addRefinement(output, ModItems.ALL_OUT_EFFORT_GU_4, 100L, 3L, 30, List.of(64, 64, 64, 64),
                Map.of('I', GuIngredient.of(Items.IRON_BLOCK, 4),
                        'B', GuIngredient.of(Items.GOLD_BLOCK, 2),
                        'A', GuIngredient.of(ModItems.ALL_OUT_EFFORT_GU_3.get(), 1)),
                " .I. ",
                "B...B",
                "I.A.I",
                "B...B",
                " .I. ");

        addRefinement(output, ModItems.WHITE_JADE_GU_2, 1L, 1L, 40, List.of(16, 16, 16, 16),
                Map.of('b', GuIngredient.of(ModItems.WHITE_BOAR_GU_1.get(), 1)),
                Map.of('j', GuIngredient.of(ModItems.JADE_SKIN_GU_1.get(), 1)),
                " ... ",
                ".jjj.",
                ".jbj.",
                ".jjj.",
                " ... ");

        addRefinement(output, ModItems.MOONGLOW_GU_2, 1L, 1L, 40, List.of(16, 16, 16, 16),
                Map.of('M', GuIngredient.of(ModItems.MOONLIGHT_GU_1.get(), 1),
                        'L', GuIngredient.of(ModItems.LITTLE_LIGHT_GU_1.get(), 1)),
                " ... ",
                ".....",
                ".LML.",
                ".....",
                " ... ");

        addSmelting(output, ModItems.ROUGH_JADE, ModItems.JADE, 0.7F);
        addSmelting(output, ModItems.ROUGH_JADEITE, ModItems.JADEITE, 0.7F);

        addBlasting(output, Items.IRON_INGOT, ModItems.STEEL_INGOT, 0.1F);
        addStorage(output, ModItems.STEEL_INGOT, ModItems.STEEL_BLOCK);
    }

    //region Cooking [烧炼]
    private static void addSmelting(RecipeOutput output, ItemLike input, ItemLike result, float experience) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, experience,
                        10 * Ticks.SECOND)
                .unlockedBy(getHasName(input), has(input))
                .save(output, Guzhenren.id(getItemName(result) + "_from_smelting_" + getItemName(input)));
        addBlasting(output, input, result, experience);
    }

    private static void addBlasting(RecipeOutput output, ItemLike input, ItemLike result, float experience) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, experience,
                        5 * Ticks.SECOND)
                .unlockedBy(getHasName(input), has(input))
                .save(output, Guzhenren.id(getItemName(result) + "_from_blasting_" + getItemName(input)));
    }
    //endregion

    //region Storage [存储块]
    private static void addStorage(RecipeOutput output, ItemLike ingot, ItemLike block) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                .define('#', ingot)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(ingot), has(ingot))
                .save(output, Guzhenren.id(getItemName(block)));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingot, 9)
                .requires(block)
                .unlockedBy(getHasName(block), has(block))
                .save(output, Guzhenren.id(getItemName(ingot) + "_from_" + getItemName(block)));
    }
    //endregion

    //region Gu Recipe patterns [蛊方图样]
    private static void addRefinement(RecipeOutput output, ItemLike result, long essencePerSecond,
                                      long soulPerSecond, int baseSuccess, List<Integer> windows,
                                      Map<Character, GuIngredient> key, String... pattern) {
        addRefinement(output, result, essencePerSecond, soulPerSecond, baseSuccess, windows, key, Map.of(), pattern);
    }

    private static void addRefinement(RecipeOutput output, ItemLike result, long essencePerSecond,
                                      long soulPerSecond, int baseSuccess, List<Integer> windows,
                                      Map<Character, GuIngredient> key, Map<Character, GuIngredient> shared,
                                      String... pattern) {
        Item item = result.asItem();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Guzhenren.MOD_ID, FOLDER + BuiltInRegistries.ITEM.getKey(item).getPath());
        List<GuIngredient> ingredients = new ArrayList<>();
        List<List<Integer>> slots = new ArrayList<>();

        read(id, key, shared, pattern, ingredients, slots);
        output.accept(id, new RefinementRecipe(new GuRecipe(List.copyOf(ingredients),
                slots.stream().map(List::copyOf).toList(), List.of(new ItemStack(item)),
                essencePerSecond, soulPerSecond, windows, baseSuccess)), null);
    }

    private static void read(ResourceLocation id, Map<Character, GuIngredient> key,
                             Map<Character, GuIngredient> shared, String[] pattern,
                             List<GuIngredient> ingredients, List<List<Integer>> slots) {
        if (pattern.length != GuRecipe.GRID_ROWS) {
            throw new IllegalArgumentException(id + " needs " + GuRecipe.GRID_ROWS + " pattern rows");
        }
        Map<Character, Integer> sharedAt = new HashMap<>();
        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            if (line.length() != GuRecipe.GRID_COLS) {
                throw new IllegalArgumentException(id + " row " + row + " is not "
                        + GuRecipe.GRID_COLS + " cells wide");
            }
            for (int col = 0; col < line.length(); col++) {
                char drawn = line.charAt(col);
                int slot = GuRecipe.getSlotAt(row, col);
                GuIngredient need = readCell(id, key, shared, drawn, slot);
                if (need == null) continue;

                Integer at = sharedAt.get(drawn);
                if (at != null) {
                    slots.get(at).add(slot);
                    continue;
                }
                if (shared.containsKey(drawn)) sharedAt.put(drawn, ingredients.size());
                ingredients.add(need);
                slots.add(new ArrayList<>(List.of(slot)));
            }
        }
        for (char letter : shared.keySet()) {
            if (!sharedAt.containsKey(letter)) throw new IllegalArgumentException(id + " never draws '" + letter + "'");
        }
    }

    private static @Nullable GuIngredient readCell(ResourceLocation id, Map<Character, GuIngredient> key,
                                                   Map<Character, GuIngredient> shared, char drawn, int slot) {
        if (slot < 0) {
            if (drawn != OUTSIDE) throw new IllegalArgumentException(id + " draws '" + drawn + "' on a cut corner");
            return null;
        }
        if (drawn == OUTSIDE) throw new IllegalArgumentException(id + " leaves a real cell blank; use '.'");
        if (drawn == EMPTY) return null;

        GuIngredient alone = key.get(drawn);
        GuIngredient pooled = shared.get(drawn);
        if (alone != null && pooled != null) {
            throw new IllegalArgumentException(id + " keys '" + drawn + "' both alone and shared");
        }
        if (alone == null && pooled == null) {
            throw new IllegalArgumentException(id + " has no ingredient keyed '" + drawn + "'");
        }
        return alone != null ? alone : pooled;
    }
    //endregion
}
