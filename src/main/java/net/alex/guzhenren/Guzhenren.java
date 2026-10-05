package net.alex.guzhenren;

import com.mojang.logging.LogUtils;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModCreativeTabs;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mod entry point: builds every registry holder and hands them to the mod event bus.
 *
 * <p>Holds the {@code MOD_ID} constant and the {@link #id} helper used across the codebase for
 * {@link ResourceLocation} creation. The constructor wires thirteen {@code DeferredRegister} holders
 * (attachments, data components, effects, fluid types, fluids, blocks, entities, items, creative
 * tabs, menus, particles, recipes, criterion triggers) to the mod event bus in the order NeoForge
 * requires. Worldgen features live in the sibling mod Gu World, which places the Spirit Spring
 * [元泉] block registered here.
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

    public Guzhenren(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModParticles.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCriteriaTriggers.register(modEventBus);
        modEventBus.addListener(EpicFightIntegration::onAnimationRegistry);
        EpicFightIntegration.initialize();
    }
}
