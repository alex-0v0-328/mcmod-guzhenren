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
 * 纯水流体：原版 {@code WaterFluid} 换上自己的注册条目，含 {@link Flowing}、{@link Source} 两变体。
 *
 * <ul>
 *   <li>水的行为全来自父类：坡度搜索 4、每格衰减 1、流动延迟 5 tick、{@code waterSourceConversion} 下两相邻源头生新源
 *       头、环境音与粒子。</li>
 *   <li>只覆写身份：流体类型、流体对、纯水桶、液体方块；流体对在 {@code #minecraft:water}，方块侧检查全当水。</li>
 *   <li>NeoForge 绑定水类型的实体侧检查由 {@code EntityMixin} 扩展；其余归 {@link PureWaterFluidType}、
 *       {@link PureWaterEvents}。</li>
 * </ul>
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
