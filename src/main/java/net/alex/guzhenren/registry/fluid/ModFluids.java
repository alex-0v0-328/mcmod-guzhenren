package net.alex.guzhenren.registry.fluid;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The Spirit Spring [元泉] fluid pair: the registry entries of the source and its flowing arm.
 *
 * <p>DeferredRegister holder only; the behavior both entries share lives in {@link SpiritSpringFluid}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModFluidTypes
 * @see SpiritSpringFluid
 * @since 1.0.0
 */

public final class ModFluids {

    private ModFluids() {}

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, Guzhenren.MOD_ID);
    public static final RegistryObject<SpiritSpringFluid.Source> SPIRIT_SPRING =
            FLUIDS.register("spirit_spring", SpiritSpringFluid.Source::new);
    public static final RegistryObject<SpiritSpringFluid.Flowing> FLOWING_SPIRIT_SPRING =
            FLUIDS.register("flowing_spirit_spring", SpiritSpringFluid.Flowing::new);

    public static void register(IEventBus modEventBus) { FLUIDS.register(modEventBus); }
}
