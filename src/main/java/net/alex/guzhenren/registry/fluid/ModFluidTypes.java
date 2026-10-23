package net.alex.guzhenren.registry.fluid;

import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

/**
 * The fluid types: the behavior half of a fluid pair, answering "what does standing in it feel
 * like". The Spirit Spring [元泉] type copies water's feel -- swim, drown, extinguish, push,
 * hydrate farmland, float boats -- and stays on every default the water experience implies,
 * except the one deliberately shut off: two adjacent sources never mint a new source.
 *
 * <p>Both fluid entries of a pair share the one type here; see {@link ModFluids}.
 *
 * <p>⚠ Loader glue: the NeoForge 1.21.1 mainline registers the type's client look in a client extension event,
 * Forge 1.20.1 asks the type itself ({@link FluidType#initializeClient}), so {@link SpringType} hands out the
 * mainline's {@link SpiritSpringClientExtensions}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModFluidTypes {

    private ModFluidTypes() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Guzhenren.MOD_ID);
    public static final RegistryObject<FluidType> SPIRIT_SPRING = FLUID_TYPES.register("spirit_spring",
            () -> new SpringType(FluidType.Properties.create()
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

    public static void register(IEventBus modEventBus) { FLUID_TYPES.register(modEventBus); }

    /** The spring's fluid type with its client look, {@link SpiritSpringClientExtensions}. */
    private static final class SpringType extends FluidType {

        private SpringType(Properties properties) { super(properties); }

        @Override
        public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new SpiritSpringClientExtensions());
        }
    }
}
