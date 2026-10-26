package net.alex.guzhenren.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

/**
 * The two vanilla-water hookups the Pure Water [纯水] cannot reach through its tag: lava meeting it hardens to
 * obsidian (a lava source) or cobblestone (flowing lava), the {@code FluidInteractionRegistry} entry NeoForge
 * keys to the water fluid type, and a dispenser pours the Pure Water Bucket [纯水桶] with the vanilla water
 * bucket's own behavior.
 *
 * <p>Annotated {@code @EventBusSubscriber}; both registries are plain maps, so the writes run in
 * {@code enqueueWork} on the main thread. Lava flowing down onto pure water turning to stone needs no entry:
 * vanilla reads that from {@code #minecraft:water}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PureWaterFluid
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PureWaterEvents {

    private PureWaterEvents() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidInteractionRegistry.addInteraction(NeoForgeMod.LAVA_TYPE.value(),
                    new FluidInteractionRegistry.InteractionInformation(ModFluidTypes.PURE_WATER.get(),
                            fluidState -> fluidState.isSource() ? Blocks.OBSIDIAN.defaultBlockState()
                                    : Blocks.COBBLESTONE.defaultBlockState()));
            DispenserBlock.registerBehavior(ModItems.PURE_WATER_BUCKET.get(),
                    DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET));
        });
    }
}
