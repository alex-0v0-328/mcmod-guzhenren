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
 * 本模组流体类型（{@link FluidType}）注册表，定义实体站在流体内部的物理手感与交互属性（具体流体对定义见
 * {@link ModFluids}）。
 * <ul>
 *   <li>元泉[Spirit Spring]沿用原版水的游泳、溺水、灭火、推动、润田与浮船等全部交互手感，但显式关闭无限水源转化
 *       （{@code canConvertToSource(false)}）。</li>
 *   <li>纯水逐项对齐 NeoForge 原版水类型属性（包含滴水石锥向炼药锅滴水），其身份重载与水标签兼容逻辑另见
 *       {@link PureWaterFluidType}。</li>
 * </ul>
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
