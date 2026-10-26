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
 * The fluid pairs: the registry entries of each source and its flowing arm, for the Spirit Spring [元泉] and
 * the Pure Water [纯水].
 *
 * <p>DeferredRegister holder only; the behavior each pair shares lives in {@link SpiritSpringFluid} and
 * {@link PureWaterFluid}.
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
