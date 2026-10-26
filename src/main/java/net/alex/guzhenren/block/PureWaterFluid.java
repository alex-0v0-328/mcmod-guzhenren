package net.alex.guzhenren.block;

import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

/**
 * The Pure Water [纯水] fluid: vanilla {@code WaterFluid} with its own registry entries, the {@link Flowing} arm
 * and the {@link Source} entry.
 *
 * <p>Everything water does comes from the superclass -- slope 4, drop-off 1, 5t flow delay, two adjacent sources
 * minting a new one under the {@code waterSourceConversion} game rule, ambient sound and particles -- so only the
 * identity is overridden: the fluid type, the pair, the Pure Water Bucket [纯水桶] and the liquid block. Pure water
 * counts as vanilla water: the pair sits in {@code #minecraft:water}, which covers every block-side check, and
 * {@code EntityMixin} extends the entity-side checks NeoForge pins to the water fluid type;
 * {@link PureWaterFluidType} and {@link PureWaterEvents} carry the rest.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModFluids
 * @see PureWaterFluidType
 * @since 1.0.0
 */

public abstract class PureWaterFluid extends WaterFluid {

    @Override
    public @NotNull FluidType getFluidType() { return ModFluidTypes.PURE_WATER.get(); }

    @Override
    public @NotNull Fluid getFlowing() { return ModFluids.FLOWING_PURE_WATER.get(); }

    @Override
    public @NotNull Fluid getSource() { return ModFluids.PURE_WATER.get(); }

    @Override
    public @NotNull Item getBucket() { return ModItems.PURE_WATER_BUCKET.get(); }

    @Override
    public @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return ModBlocks.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == ModFluids.PURE_WATER.get() || fluid == ModFluids.FLOWING_PURE_WATER.get();
    }

    public static class Flowing extends PureWaterFluid {

        @Override
        protected void createFluidStateDefinition(StateDefinition.@NotNull Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }

        @Override
        public boolean isSource(@NotNull FluidState state) { return false; }
    }

    public static class Source extends PureWaterFluid {

        @Override
        public int getAmount(@NotNull FluidState state) { return 8; }

        @Override
        public boolean isSource(@NotNull FluidState state) { return true; }
    }
}
