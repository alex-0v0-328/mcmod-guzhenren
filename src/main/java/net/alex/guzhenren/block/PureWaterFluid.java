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
 * 纯水流体实现：继承自原版 {@link WaterFluid} 并替换为本模组注册条目，包含静态源头（{@link Source}）与流动变体
 * （{@link Flowing}）。
 * <ul>
 *   <li>完整继承原版水的物理与表现：坡度搜寻距离 4、每格水位衰减 1、流动间隔 5 tick、遵循 {@code waterSourceConversion}
 *       规则的无限水源转化，以及水下环境音效与滴水粒子。</li>
 *   <li>仅覆写身份关联方法，绑定本模组的 {@link ModFluidTypes#PURE_WATER}、正流与源流注册项、纯水桶及液体方块；由于流体
 *       对已加入 {@code #minecraft:water} 标签，方块侧的水体判定均直接将其视作水。</li>
 *   <li>NeoForge 按具体水类型判断的实体交互与属性分别由 {@link PureWaterFluidType}、{@link PureWaterEvents} 与
 *       {@code EntityMixin} 补齐。</li>
 * </ul>
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
