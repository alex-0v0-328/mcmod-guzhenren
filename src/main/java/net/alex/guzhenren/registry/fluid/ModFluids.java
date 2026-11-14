package net.alex.guzhenren.registry.fluid;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.PureWaterFluid;
import net.alex.guzhenren.block.SpiritSpringFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 元泉[Spirit Spring]与纯水的流体对注册（源头与流动态），仅 DeferredRegister 容器；共享行为在
 * {@link SpiritSpringFluid}、{@link PureWaterFluid}。
 *
 * @author Alex
 * @version 1.0.0
 * @see ModFluidTypes
 * @see SpiritSpringFluid
 * @see PureWaterFluid
 * @since 1.0.0
 */

public final class ModFluids {

    private ModFluids() {}

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Guzhenren.MOD_ID);
    public static final DeferredHolder<Fluid, SpiritSpringFluid.Source> SPIRIT_SPRING =
            FLUIDS.register("spirit_spring", SpiritSpringFluid.Source::new);
    public static final DeferredHolder<Fluid, SpiritSpringFluid.Flowing> FLOWING_SPIRIT_SPRING =
            FLUIDS.register("flowing_spirit_spring", SpiritSpringFluid.Flowing::new);
    public static final DeferredHolder<Fluid, PureWaterFluid.Source> PURE_WATER =
            FLUIDS.register("pure_water", PureWaterFluid.Source::new);
    public static final DeferredHolder<Fluid, PureWaterFluid.Flowing> FLOWING_PURE_WATER =
            FLUIDS.register("flowing_pure_water", PureWaterFluid.Flowing::new);

    public static void register(IEventBus modEventBus) { FLUIDS.register(modEventBus); }
}
