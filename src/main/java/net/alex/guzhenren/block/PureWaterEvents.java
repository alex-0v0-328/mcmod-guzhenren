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
 * 纯水流体无法仅靠流体标签覆盖的两处原版交互注册入口。
 * <ul>
 *   <li>在 {@link FMLCommonSetupEvent} 中通过 {@code enqueueWork} 向主线程注册岩浆接触纯水的流体交互（纯水源头变黑曜
 *       石、流动纯水变圆石，因 NeoForge 将 {@link FluidInteractionRegistry} 绑定在具体 {@code FluidType} 而非标签上；岩
 *       浆自上而下流入纯水生成石头则已由原版 {@code #minecraft:water} 标签覆盖）。</li>
 *   <li>同时为纯水桶注册与原版水桶一致的发射器倒水行为（{@link DispenserBlock#registerBehavior}）。</li>
 * </ul>
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
