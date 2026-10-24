package net.alex.guzhenren.refinement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The recipe manager's view of a {@link GuRecipe}: the vanilla {@link Recipe} it registers as, its serializer, and
 * the sorted listing every refinement caller reads.
 *
 * <p>⚠ A version seam: the recipe interface, the serializer and the recipe manager's lookup differ between the
 * NeoForge 1.21.1 mainline and the Forge 1.20.1 port; {@link GuRecipe}, {@link GuIngredient} and {@link
 * GuRecipeInput} are the same on both. Nothing outside this class touches the vanilla recipe API.
 *
 * <p>⚠ A recipe index means the same thing on both sides only because {@link #getKnown} sorts by recipe id. The
 * recipe manager's own order guarantees nothing at all.
 *
 * <p>⚠ The JSON matches the mainline's: an ingredient is the flat object {@code {"count": n, "item": …}}, and a
 * field at its default (no essence, no soul, no windows, base success 100) is left out. Only a result reads
 * {@code item} where the mainline reads {@code id}.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public record RefinementRecipe(ResourceLocation id, GuRecipe gu) implements Recipe<Container> {

    private static final long NO_COST = 0L;
    private static final int FULL_SUCCESS = 100;

    //region the Gu Recipe [蛊方] a player may attempt -- sorted, so an index means the same on both sides
    public static List<GuRecipe> getKnown(RecipeManager recipes) {
        return recipes.getAllRecipesFor(ModRecipes.REFINEMENT.get()).stream()
                .sorted(Comparator.comparing(RefinementRecipe::getId))
                .map(RefinementRecipe::gu)
                .toList();
    }
    //endregion

    //region what the recipe manager reads
    @Override
    public boolean matches(@NotNull Container input, @NotNull Level level) {
        return gu.claim(GuRecipeInput.of(input)) != null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public @NotNull ResourceLocation getId() { return id; }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.REFINEMENT_SERIALIZER.get(); }

    @Override
    public @NotNull RecipeType<?> getType() { return ModRecipes.REFINEMENT.get(); }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container input, @NotNull RegistryAccess registries) {
        return gu.getFirst().copy();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registries) { return gu.getFirst(); }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (GuIngredient need : gu.ingredients()) list.add(need.ingredient());
        return list;
    }
    //endregion

    public static final class Serializer implements RecipeSerializer<RefinementRecipe> {

        @Override
        public @NotNull RefinementRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            List<GuIngredient> ingredients = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
                JsonObject need = GsonHelper.convertToJsonObject(element, "ingredient");
                ingredients.add(new GuIngredient(Ingredient.fromJson(need, false),
                        GsonHelper.getAsInt(need, "count", 1)));
            }
            List<ItemStack> results = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "results")) {
                results.add(ShapedRecipe.itemStackFromJson(GsonHelper.convertToJsonObject(element, "result")));
            }
            return new RefinementRecipe(id, new GuRecipe(List.copyOf(ingredients), ints(json, "slots"),
                    List.copyOf(results),
                    GsonHelper.getAsLong(json, "essence_per_second", NO_COST),
                    GsonHelper.getAsLong(json, "soul_per_second", NO_COST),
                    json.has("windows") ? ints(json, "windows") : List.of(),
                    GsonHelper.getAsInt(json, "base_success", FULL_SUCCESS)));
        }

        private static List<Integer> ints(JsonObject json, String key) {
            List<Integer> values = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, key)) {
                values.add(GsonHelper.convertToInt(element, key));
            }
            return List.copyOf(values);
        }

        @Override
        public @NotNull RefinementRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
            List<GuIngredient> ingredients = buf.readList(b -> new GuIngredient(Ingredient.fromNetwork(b),
                    b.readVarInt()));
            List<Integer> slots = buf.readList(FriendlyByteBuf::readVarInt);
            List<ItemStack> results = buf.readList(FriendlyByteBuf::readItem);
            long essence = buf.readVarLong();
            long soul = buf.readVarLong();
            List<Integer> windows = buf.readList(FriendlyByteBuf::readVarInt);
            int success = buf.readVarInt();
            return new RefinementRecipe(id, new GuRecipe(ingredients, slots, results, essence, soul, windows,
                    success));
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull RefinementRecipe recipe) {
            GuRecipe gu = recipe.gu();
            buf.writeCollection(gu.ingredients(), (b, need) -> {
                need.ingredient().toNetwork(b);
                b.writeVarInt(need.count());
            });
            buf.writeCollection(gu.slots(), FriendlyByteBuf::writeVarInt);
            buf.writeCollection(gu.results(), FriendlyByteBuf::writeItem);
            buf.writeVarLong(gu.essencePerSecond());
            buf.writeVarLong(gu.soulPerSecond());
            buf.writeCollection(gu.windows(), FriendlyByteBuf::writeVarInt);
            buf.writeVarInt(gu.baseSuccess());
        }
    }

    /** What datagen writes for one Gu Recipe; the same JSON the serializer reads. */
    public record Finished(ResourceLocation id, GuRecipe gu) implements FinishedRecipe {

        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            JsonArray ingredients = new JsonArray();
            for (GuIngredient need : gu.ingredients()) {
                JsonObject cell = need.ingredient().toJson().getAsJsonObject();
                cell.addProperty("count", need.count());
                ingredients.add(cell);
            }
            json.add("ingredients", ingredients);
            json.add("slots", array(gu.slots()));

            JsonArray results = new JsonArray();
            for (ItemStack stack : gu.results()) {
                JsonObject result = new JsonObject();
                result.addProperty("count", stack.getCount());
                result.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                results.add(result);
            }
            json.add("results", results);

            if (gu.essencePerSecond() != NO_COST) json.addProperty("essence_per_second", gu.essencePerSecond());
            if (gu.soulPerSecond() != NO_COST) json.addProperty("soul_per_second", gu.soulPerSecond());
            if (!gu.windows().isEmpty()) json.add("windows", array(gu.windows()));
            if (gu.baseSuccess() != FULL_SUCCESS) json.addProperty("base_success", gu.baseSuccess());
        }

        private static JsonArray array(List<Integer> values) {
            JsonArray array = new JsonArray();
            values.forEach(array::add);
            return array;
        }

        @Override
        public @NotNull ResourceLocation getId() { return id; }

        @Override
        public @NotNull RecipeSerializer<?> getType() { return ModRecipes.REFINEMENT_SERIALIZER.get(); }

        @Override
        public @Nullable JsonObject serializeAdvancement() { return null; }

        @Override
        public @Nullable ResourceLocation getAdvancementId() { return null; }
    }
}
