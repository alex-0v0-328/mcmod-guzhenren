package net.alex.guzhenren.refinement.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import org.jetbrains.annotations.NotNull;

/**
 * The recipe manager's view of a {@link GuRecipe}: the vanilla {@link Recipe} it registers as, its serializer, and
 * the sorted listing every refinement caller reads.
 *
 * <p>⚠ Nothing outside this class touches the vanilla recipe API; {@link GuRecipe}, {@link GuIngredient} and {@link
 * GuRecipeInput} never name it.
 *
 * <p>⚠ A recipe index means the same thing on both sides only because {@link #getKnown} sorts by recipe id. The
 * recipe manager's own order guarantees nothing at all.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public record RefinementRecipe(GuRecipe gu) implements Recipe<RecipeInput> {

    //region the Gu Recipe [蛊方] a player may attempt -- sorted, so an index means the same on both sides
    public static List<GuRecipe> getKnown(RecipeManager recipes) {
        return recipes.getAllRecipesFor(ModRecipes.REFINEMENT.get()).stream()
                .sorted(Comparator.comparing(RecipeHolder::id))
                .map(held -> held.value().gu())
                .toList();
    }
    //endregion

    //region what the recipe manager reads
    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        List<ItemStack> slots = new ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) slots.add(input.getItem(i));
        return gu.claim(new GuRecipeInput(List.copyOf(slots))) != null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.REFINEMENT_SERIALIZER.get(); }

    @Override
    public @NotNull RecipeType<?> getType() { return ModRecipes.REFINEMENT.get(); }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider registries) {
        return gu.getFirst().copy();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) { return gu.getFirst(); }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (GuIngredient need : gu.ingredients()) list.add(need.ingredient());
        return list;
    }
    //endregion

    public static final class Serializer implements RecipeSerializer<RefinementRecipe> {

        private static final Codec<GuIngredient> NEED = RecordCodecBuilder.create(i -> i.group(
                Ingredient.MAP_CODEC_NONEMPTY.forGetter(GuIngredient::ingredient),
                NeoForgeExtraCodecs.optionalFieldAlwaysWrite(ExtraCodecs.POSITIVE_INT, "count", 1)
                        .forGetter(GuIngredient::count)
        ).apply(i, GuIngredient::new));

        // a one-cell group stays a bare number, so a fixed-cell recipe file reads the same as before groups
        private static final Codec<List<Integer>> CELLS = Codec.either(Codec.INT, Codec.INT.listOf()).xmap(
                either -> either.map(List::of, Function.identity()),
                cells -> cells.size() == 1 ? Either.left(cells.getFirst()) : Either.right(cells));

        private static final MapCodec<RefinementRecipe> CODEC = RecordCodecBuilder.<GuRecipe>mapCodec(i -> i.group(
                NEED.listOf().fieldOf("ingredients").forGetter(GuRecipe::ingredients),
                CELLS.listOf().fieldOf("slots").forGetter(GuRecipe::slots),
                ItemStack.CODEC.listOf().fieldOf("results").forGetter(GuRecipe::results),
                Codec.LONG.optionalFieldOf("essence_per_second", 0L).forGetter(GuRecipe::essencePerSecond),
                Codec.LONG.optionalFieldOf("soul_per_second", 0L).forGetter(GuRecipe::soulPerSecond),
                Codec.INT.listOf().optionalFieldOf("windows", List.of()).forGetter(GuRecipe::windows),
                Codec.INT.optionalFieldOf("base_success", 100).forGetter(GuRecipe::baseSuccess)
        ).apply(i, GuRecipe::new)).xmap(RefinementRecipe::new, RefinementRecipe::gu);

        private static final StreamCodec<RegistryFriendlyByteBuf, GuIngredient> NEED_STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, GuIngredient::ingredient,
                ByteBufCodecs.VAR_INT, GuIngredient::count,
                GuIngredient::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, List<GuIngredient>> NEEDS =
                NEED_STREAM.apply(ByteBufCodecs.list());
        private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS =
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        private static final StreamCodec<ByteBuf, List<Integer>> INTS =
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());
        private static final StreamCodec<ByteBuf, List<List<Integer>>> GROUPS = INTS.apply(ByteBufCodecs.list());

        private static final StreamCodec<RegistryFriendlyByteBuf, RefinementRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public @NotNull RefinementRecipe decode(@NotNull RegistryFriendlyByteBuf buf) {
                        return new RefinementRecipe(new GuRecipe(
                                NEEDS.decode(buf),
                                GROUPS.decode(buf),
                                STACKS.decode(buf),
                                ByteBufCodecs.VAR_LONG.decode(buf),
                                ByteBufCodecs.VAR_LONG.decode(buf),
                                INTS.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf)));
                    }

                    @Override
                    public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull RefinementRecipe value) {
                        GuRecipe gu = value.gu();
                        NEEDS.encode(buf, gu.ingredients());
                        GROUPS.encode(buf, gu.slots());
                        STACKS.encode(buf, gu.results());
                        ByteBufCodecs.VAR_LONG.encode(buf, gu.essencePerSecond());
                        ByteBufCodecs.VAR_LONG.encode(buf, gu.soulPerSecond());
                        INTS.encode(buf, gu.windows());
                        ByteBufCodecs.VAR_INT.encode(buf, gu.baseSuccess());
                    }
                };

        @Override
        public @NotNull MapCodec<RefinementRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, RefinementRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
