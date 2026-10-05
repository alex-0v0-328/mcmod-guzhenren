package net.alex.guzhenren.datagen.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.refinement.GuRecipe;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

/**
 * Writes the refinement [炼蛊] recipes, each one drawn as an ASCII picture of the grid.
 *
 * <p>Extends {@link net.minecraft.data.recipes.RecipeProvider}. Each recipe is transcribed verbatim
 * from an ASCII drawing ({@code '.'} empty, {@code ' '} cut corner), so a wrong corner, a short row
 * or an unkeyed letter fails datagen. Output lands under {@code data/guzhenren/recipe/refinement/}.
 *
 * <p>⚠ The drawing is transcribed verbatim, so a wrong corner, a short row or an unkeyed letter fails
 * datagen. The layout and every number in it are Alex's.
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
        refinement(output, ModItems.FOUR_FLAVORS_LIQUOR_WORM, 1L, 1L, 50, List.of(16, 16, 16, 16),
                Map.of('s', SizedIngredient.of(ModItems.SOUR_LIQUOR.get(), 1),
                        'b', SizedIngredient.of(ModItems.BITTER_LIQUOR.get(), 1),
                        'h', SizedIngredient.of(ModItems.SPICY_LIQUOR.get(), 1),
                        'w', SizedIngredient.of(ModItems.SWEET_LIQUOR.get(), 1),
                        'g', SizedIngredient.of(ModItems.LIQUOR_WORM.get(), 1)),
                " .s. ",
                ".....",
                "bg.gh",
                ".....",
                " .w. ");

        refinement(output, ModItems.ALL_OUT_EFFORT_GU_4, 100L, 3L, 30, List.of(64, 64, 64, 64),
                Map.of('I', SizedIngredient.of(Items.IRON_BLOCK, 4),
                        'B', SizedIngredient.of(Items.GOLD_BLOCK, 2),
                        'A', SizedIngredient.of(ModItems.ALL_OUT_EFFORT_GU_3.get(), 1)),
                " .I. ",
                "B...B",
                "I.A.I",
                "B...B",
                " .I. ");
    }

    //region Gu Recipe [蛊方] patterns -- one row a grid row, ' ' a cut corner, '.' an empty cell
    private static void refinement(RecipeOutput output, ItemLike result, long essencePerSecond,
                                   long soulPerSecond, int baseSuccess, List<Integer> windows,
                                   Map<Character, SizedIngredient> key, String... pattern) {
        Item item = result.asItem();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Guzhenren.MOD_ID, FOLDER + BuiltInRegistries.ITEM.getKey(item).getPath());
        List<SizedIngredient> ingredients = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();

        read(id, key, pattern, ingredients, slots);
        output.accept(id, new GuRecipe(List.copyOf(ingredients), List.copyOf(slots),
                List.of(new ItemStack(item)), essencePerSecond, soulPerSecond, windows, baseSuccess), null);
    }

    private static void read(ResourceLocation id, Map<Character, SizedIngredient> key, String[] pattern,
                             List<SizedIngredient> ingredients, List<Integer> slots) {
        if (pattern.length != GuRecipe.GRID_ROWS) {
            throw new IllegalArgumentException(id + " needs " + GuRecipe.GRID_ROWS + " pattern rows");
        }
        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            if (line.length() != GuRecipe.GRID_COLS) {
                throw new IllegalArgumentException(id + " row " + row + " is not "
                        + GuRecipe.GRID_COLS + " cells wide");
            }
            for (int col = 0; col < line.length(); col++) {
                cell(id, key, line.charAt(col), GuRecipe.slotAt(row, col), ingredients, slots);
            }
        }
    }

    private static void cell(ResourceLocation id, Map<Character, SizedIngredient> key, char drawn, int slot,
                             List<SizedIngredient> ingredients, List<Integer> slots) {
        if (slot < 0) {
            if (drawn != OUTSIDE) throw new IllegalArgumentException(id + " draws '" + drawn + "' on a cut corner");
            return;
        }
        if (drawn == OUTSIDE) throw new IllegalArgumentException(id + " leaves a real cell blank; use '.'");
        if (drawn == EMPTY) return;

        SizedIngredient need = key.get(drawn);
        if (need == null) throw new IllegalArgumentException(id + " has no ingredient keyed '" + drawn + "'");

        ingredients.add(need);
        slots.add(slot);
    }
    //endregion
}
