package net.alex.guzhenren;

import com.mojang.logging.LogUtils;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.registry.capability.ModCapabilities;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod entry point: builds every registry holder and hands them to the mod event bus.
 *
 * <p>Holds the {@code MOD_ID} constant and the {@link #id} helper used across the codebase for
 * {@link ResourceLocation} creation. The constructor takes the mod event bus from the loading context
 * Forge injects and wires each {@code DeferredRegister} holder to it in the order Forge requires, then
 * the player-data capability and the network channel.
 * Worldgen features live in the sibling mod Gu World, which places the Spirit Spring [元泉] block
 * registered here.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(Guzhenren.MOD_ID)
public class Guzhenren {

    public static final String MOD_ID = "guzhenren";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    public Guzhenren(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModEffects.register(modEventBus);
        ModCapabilities.register(modEventBus);
        ModNetwork.register();
    }
}
