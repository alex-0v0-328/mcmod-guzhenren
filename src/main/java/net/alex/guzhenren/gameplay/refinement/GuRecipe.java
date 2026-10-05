package net.alex.guzhenren.gameplay.refinement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One Gu recipe [蛊方]: what it eats, what it yields, and the windows the ritual runs through.
 *
 * <p>Implements {@link net.minecraft.world.item.crafting.Recipe} for {@link GuRecipeInput}. The
 * ingredients and their cell positions are parallel lists (SHAPED and exact -- no mirroring, no
 * rotation); the window list's length IS the stage count. The serializer, the recipe type, and the
 * matching logic all live here.
 *
 * <p>⚠ A recipe index means the same thing on both sides only because the listing sorts by recipe id.
 * The recipe manager's own order guarantees nothing at all.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementMenu
 * @since 1.0.0
 */

public record GuRecipe(List<SizedIngredient> ingredients, List<Integer> slots, List<ItemStack> results,
                       long essencePerSecond, long soulPerSecond, List<Integer> windows, int baseSuccess)
        implements Recipe<GuRecipeInput> {

    public static final int WINDOW_TICKS = 5 * Ticks.SECOND;
    public static final int GAP_TICKS = 2 * Ticks.SECOND;

    public GuRecipe {
        if (essencePerSecond < 0L) throw new IllegalArgumentException("essencePerSecond must be non-negative");
        if (soulPerSecond < 0L) throw new IllegalArgumentException("soulPerSecond must be non-negative");
        if (windows.stream().anyMatch(window -> window < 0)) {
            throw new IllegalArgumentException("windows must be non-negative");
        }
        if (baseSuccess < 0 || baseSuccess > 100) {
            throw new IllegalArgumentException("baseSuccess must be 0..100");
        }
        if (ingredients.size() != slots.size()) {
            throw new IllegalArgumentException("every ingredient owns exactly one cell of the grid");
        }
        try {
            long totalSeconds = exactTotalTicks(windows.size()) / Ticks.SECOND;
            if (Math.addExact(Math.multiplyExact(essencePerSecond, totalSeconds),
                    Math.multiplyExact(soulPerSecond, totalSeconds)) < 0L) {
                throw new ArithmeticException("combined cost overflow");
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("recipe runtime exceeds supported range", e);
        }
    }

    //region the ritual's own clock -- the window list IS the stage count
    public int windowCount() { return windows.size(); }

    public int stonesFor(int window) { return windows.get(window); }

    public int totalSeconds() { return totalTicks() / Ticks.SECOND; }

    public long essenceToFinish() { return Math.multiplyExact(essencePerSecond, totalSeconds()); }

    public int totalTicks() {
        return exactTotalTicks(windowCount());
    }

    private static int exactTotalTicks(int windows) {
        return Math.addExact(Math.multiplyExact(windows, WINDOW_TICKS),
                Math.multiplyExact(Math.max(0, windows - 1), GAP_TICKS));
    }
    //endregion

    //region the Gu Recipe [蛊方] a player may attempt -- sorted, so an index means the same on both sides
    public static List<RecipeHolder<GuRecipe>> known(RecipeManager recipes) {
        return recipes.getAllRecipesFor(ModRecipes.REFINEMENT.get()).stream()
                .sorted(Comparator.comparing(RecipeHolder::id))
                .toList();
    }
    //endregion

    //region matching -- SHAPED and EXACT: every cell holds what its own ingredient asks for, or nothing
    public int @Nullable [] claim(GuRecipeInput input) {
        int[] taken = new int[input.size()];

        for (int n = 0; n < ingredients.size(); n++) {
            int slot = slots.get(n);
            if (slot < 0 || slot >= input.size()) return null;

            SizedIngredient need = ingredients.get(n);
            if (!need.ingredient().test(input.getItem(slot))) return null;

            taken[slot] += need.count();
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).getCount() != taken[i]) return null;
        }
        return taken;
    }

    public int[] shortfall(GuRecipeInput input) {
        int[] missing = new int[ingredients.size()];

        for (int n = 0; n < ingredients.size(); n++) {
            SizedIngredient need = ingredients.get(n);
            ItemStack held = held(input, slots.get(n));
            int have = need.ingredient().test(held) ? held.getCount() : 0;
            missing[n] = Math.max(0, need.count() - have);
        }
        return missing;
    }

    private static ItemStack held(GuRecipeInput input, int slot) {
        return slot < 0 || slot >= input.size() ? ItemStack.EMPTY : input.getItem(slot);
    }
    //endregion

    //region the Gu count among the results -- the sole-Gu inheritance rule reads it
    //  TODO(炼法): rebuild the refinement-mode derivation (two axes, never one enum) once modes gain behavior.
    public int guResultCount() {
        int n = 0;
        for (ItemStack stack : results) {
            if (stack.getItem() instanceof MortalGuItem) n += stack.getCount();
        }
        return n;
    }
    //endregion

    //region what the recipe manager reads
    @Override
    public boolean matches(@NotNull GuRecipeInput input, @NotNull Level level) { return claim(input) != null; }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.REFINEMENT_SERIALIZER.get(); }

    @Override
    public @NotNull RecipeType<?> getType() { return ModRecipes.REFINEMENT.get(); }

    @Override
    public @NotNull ItemStack assemble(@NotNull GuRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return first().copy();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) { return first(); }

    private ItemStack first() { return results.isEmpty() ? ItemStack.EMPTY : results.getFirst(); }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (SizedIngredient need : ingredients) list.add(need.ingredient());
        return list;
    }
    //endregion

    public static final class Serializer implements RecipeSerializer<GuRecipe> {

        private static final MapCodec<GuRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(GuRecipe::ingredients),
                Codec.INT.listOf().fieldOf("slots").forGetter(GuRecipe::slots),
                ItemStack.CODEC.listOf().fieldOf("results").forGetter(GuRecipe::results),
                Codec.LONG.optionalFieldOf("essence_per_second", 0L).forGetter(GuRecipe::essencePerSecond),
                Codec.LONG.optionalFieldOf("soul_per_second", 0L).forGetter(GuRecipe::soulPerSecond),
                Codec.INT.listOf().optionalFieldOf("windows", List.of()).forGetter(GuRecipe::windows),
                Codec.INT.optionalFieldOf("base_success", 100).forGetter(GuRecipe::baseSuccess)
        ).apply(i, GuRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, List<SizedIngredient>> NEEDS =
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list());
        private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS =
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        private static final StreamCodec<ByteBuf, List<Integer>> INTS =
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());

        private static final StreamCodec<RegistryFriendlyByteBuf, GuRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public @NotNull GuRecipe decode(@NotNull RegistryFriendlyByteBuf buf) {
                        return new GuRecipe(
                                NEEDS.decode(buf),
                                INTS.decode(buf),
                                STACKS.decode(buf),
                                ByteBufCodecs.VAR_LONG.decode(buf),
                                ByteBufCodecs.VAR_LONG.decode(buf),
                                INTS.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf));
                    }

                    @Override
                    public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull GuRecipe value) {
                        NEEDS.encode(buf, value.ingredients());
                        INTS.encode(buf, value.slots());
                        STACKS.encode(buf, value.results());
                        ByteBufCodecs.VAR_LONG.encode(buf, value.essencePerSecond());
                        ByteBufCodecs.VAR_LONG.encode(buf, value.soulPerSecond());
                        INTS.encode(buf, value.windows());
                        ByteBufCodecs.VAR_INT.encode(buf, value.baseSuccess());
                    }
                };

        @Override
        public @NotNull MapCodec<GuRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, GuRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
