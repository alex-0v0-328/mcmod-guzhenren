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
 * 纯水靠标签够不到的两处原版水挂钩。
 *
 * <ul>
 *   <li>岩浆遇纯水（源头成黑曜石，流动成圆石）：NeoForge 把 {@code FluidInteractionRegistry} 这条目绑在水类型上。</li>
 *   <li>发射器倒纯水桶沿用原版水桶行为；两注册表都是普通映射，写入经 {@code enqueueWork} 在主线程执行。</li>
 *   <li>岩浆从上方流到纯水上变石头无需条目：原版读 {@code #minecraft:water}。</li>
 * </ul>
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
