package net.alex.guzhenren.block;

import java.util.Optional;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

/**
 * 元泉[Spirit Spring]流体：流体对两端共有行为，含 {@link Flowing}、{@link Source} 两变体。
 *
 * <ul>
 *   <li>照搬原版水形态：坡度搜索 4、每格衰减 1、流动延迟 5 tick、水下环境音、滴水粒子；去掉无限水源
 *       （{@code canConvertToSource} 恒 false），元泉只在放过源头处存在。</li>
 *   <li>无专用桶；原版空桶舀取清掉元泉，原样还回空桶（{@link #getBucket}）。</li>
 *   <li>⚠ 源头随机 tick 只是重载看门狗：重挂 {@link SpiritSpringBlock} 的确定性方块 tick 链，自
 *       身不产元石[Primeval Stone]。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ModFluids
 * @see ModFluidTypes
 * @see SpiritSpringBlock
 * @since 1.0.0
 */

public abstract class SpiritSpringFluid extends FlowingFluid {

    @Override
    public @NotNull FluidType getFluidType() { return ModFluidTypes.SPIRIT_SPRING.get(); }

    @Override
    public @NotNull Fluid getFlowing() { return ModFluids.FLOWING_SPIRIT_SPRING.get(); }

    @Override
    public @NotNull Fluid getSource() { return ModFluids.SPIRIT_SPRING.get(); }

    @Override
    public @NotNull Item getBucket() { return Items.BUCKET; }

    @Override
    public void animateTick(@NotNull Level level, @NotNull BlockPos pos,
                            FluidState state, @NotNull RandomSource random) {
        if (!state.isSource() && !state.getValue(FALLING)) {
            if (random.nextInt(64) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS,
                        random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F, false);
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.UNDERWATER, pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected ParticleOptions getDripParticle() { return ParticleTypes.DRIPPING_WATER; }

    @Override
    protected boolean canConvertToSource(@NotNull Level level) { return false; }

    @Override
    protected void beforeDestroyingBlock(@NotNull LevelAccessor level, @NotNull BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    public int getSlopeFindDistance(@NotNull LevelReader level) { return 4; }

    @Override
    public @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return ModBlocks.SPIRIT_SPRING.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == ModFluids.SPIRIT_SPRING.get() || fluid == ModFluids.FLOWING_SPIRIT_SPRING.get();
    }

    @Override
    public int getDropOff(@NotNull LevelReader level) { return 1; }

    @Override
    public int getTickDelay(@NotNull LevelReader level) { return 5; }

    @Override
    public boolean canBeReplacedWith(@NotNull FluidState fluidState, @NotNull BlockGetter blockReader,
                                     @NotNull BlockPos pos, @NotNull Fluid fluid, @NotNull Direction direction) {
        return direction == Direction.DOWN && !fluid.isSame(this);
    }

    @Override
    protected float getExplosionResistance() { return 100.0F; }

    @Override
    public @NotNull Optional<SoundEvent> getPickupSound() { return Optional.of(SoundEvents.BUCKET_FILL); }

    public static class Flowing extends SpiritSpringFluid {

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

    public static class Source extends SpiritSpringFluid {

        @Override
        public int getAmount(@NotNull FluidState state) { return 8; }

        @Override
        public boolean isSource(@NotNull FluidState state) { return true; }

        @Override
        protected boolean isRandomlyTicking() { return true; }

        @Override
        protected void randomTick(@NotNull Level level, @NotNull BlockPos pos,
                                  @NotNull FluidState state, @NotNull RandomSource random) {
            ModBlocks.SPIRIT_SPRING.get().scheduleProduction(level, pos);
        }
    }
}
