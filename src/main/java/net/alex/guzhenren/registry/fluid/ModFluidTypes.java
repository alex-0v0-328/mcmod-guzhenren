package net.alex.guzhenren.registry.fluid;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.PureWaterFluidType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 流体类型：流体对的行为一半，即「站在里面的感觉」。
 *
 * <ul>
 *   <li>元泉[Spirit Spring]照搬水的手感（游泳、溺水、灭火、推动、润田、浮船）及默认值，仅关两源生新源。</li>
 *   <li>纯水逐项等同 NeoForge 水类型，含滴水石锥滴入水炼药锅；身份覆盖在 {@link PureWaterFluidType}。
 *       一对流体的两个条目共用同一类型，见 {@link ModFluids}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModFluidTypes {

    private ModFluidTypes() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Guzhenren.MOD_ID);
    public static final Supplier<FluidType> SPIRIT_SPRING = FLUID_TYPES.register("spirit_spring",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.guzhenren.spirit_spring")
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .canPushEntity(true)
                    .canHydrate(true)
                    .supportsBoating(true)
                    .canConvertToSource(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final Supplier<FluidType> PURE_WATER = FLUID_TYPES.register("pure_water",
            () -> new PureWaterFluidType(FluidType.Properties.create()
                    .descriptionId("block.guzhenren.pure_water")
                    .fallDistanceModifier(0.0F)
                    .canExtinguish(true)
                    .canConvertToSource(true)
                    .supportsBoating(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                    .canHydrate(true)
                    .addDripstoneDripping(PointedDripstoneBlock.WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK,
                            ParticleTypes.DRIPPING_DRIPSTONE_WATER, Blocks.WATER_CAULDRON,
                            SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON)));

    public static void register(IEventBus modEventBus) { FLUID_TYPES.register(modEventBus); }
}
