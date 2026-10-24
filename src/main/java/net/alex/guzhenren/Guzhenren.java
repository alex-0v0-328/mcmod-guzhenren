package net.alex.guzhenren;

import com.mojang.logging.LogUtils;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.StonePurse;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.StoredGuItems;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.capability.ModCapabilities;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModCreativeTabs;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.loot.ModLootModifiers;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod entry point: builds every registry holder and hands them to the mod event bus.
 *
 * <p>Holds the {@code MOD_ID} constant and the {@link #id} helper used across the codebase for
 * {@link ResourceLocation} creation. The constructor takes the mod event bus from the loading context Forge injects
 * and wires each {@code DeferredRegister} holder to it in the order Forge requires (effects, fluid types, fluids,
 * blocks, entities, items, creative tabs, menus, particles, recipes, loot modifiers, criterion triggers), then the
 * player-data capability and the network channel. It also hands the aperture's nourishing the {@code StonePurse}
 * built on the item side's {@code PrimevalStoneSupply}, and aperture storage the {@code StoredGu} built on the Gu
 * item classes ({@code StoredGuItems}), so the gameplay packages never depend on the item package. Worldgen
 * features live in the sibling mod Gu World, which places the Spirit Spring [元泉] block registered here.
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
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModParticles.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModCriteriaTriggers.register(modEventBus);
        ApertureNourishService.setStonePurse(new StonePurse(
                PrimevalStoneSupply::canAfford, PrimevalStoneSupply::topUp, PrimevalStoneSupply::trySpend));
        ApertureStorageService.setStoredGu(new StoredGuItems());
        modEventBus.addListener(EpicFightIntegration::onAnimationRegistry);
        EpicFightIntegration.initialize();
        ModCapabilities.register(modEventBus);
        ModNetwork.register();
    }
}
