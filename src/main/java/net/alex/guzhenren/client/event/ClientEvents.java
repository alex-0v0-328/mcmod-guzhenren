package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.hud.ChargeHud;
import net.alex.guzhenren.client.hud.NourishHud;
import net.alex.guzhenren.client.hud.PlayerStatsHud;
import net.alex.guzhenren.client.input.DashInput;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.client.renderer.ModEntityRenderers;
import net.alex.guzhenren.client.screen.ApertureStorageScreen;
import net.alex.guzhenren.client.screen.PlayerInfoScreen;
import net.alex.guzhenren.client.screen.RefinementScreen;
import net.alex.guzhenren.client.screen.SoulTradeScreen;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Every client-side registration this mod makes: HUD layers, key mappings, screens and renderers.
 *
 * <p>Annotated {@code @EventBusSubscriber(Dist.CLIENT)}. Registers three GUI layers
 * ({@link net.alex.guzhenren.client.hud.PlayerStatsHud},
 * {@link net.alex.guzhenren.client.hud.ChargeHud},
 * {@link net.alex.guzhenren.client.hud.NourishHud}), the key mapping for the B panel, the menu
 * screens for the three containers, the shockwave-ring particle provider, the entity renderers
 * ({@link ModEntityRenderers}) and the Spirit Spring fluid's translucent render layer.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.ModKeyMappings
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    private static final ResourceLocation PLAYER_STATS =
            Guzhenren.id("player_stats");
    private static final ResourceLocation CHARGE =
            Guzhenren.id("charge");
    private static final ResourceLocation NOURISH =
            Guzhenren.id("nourish");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, PLAYER_STATS, PlayerStatsHud.INSTANCE);
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, CHARGE, ChargeHud.INSTANCE);
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, NOURISH, NourishHud.INSTANCE);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_INFO);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.APERTURE_STORAGE_MENU.get(), ApertureStorageScreen::new);
        event.register(ModMenus.REFINEMENT_MENU.get(), RefinementScreen::new);
        event.register(ModMenus.SOUL_TRADE_MENU.get(), SoulTradeScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), RingParticle::dashTrail);
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), RingParticle::facingMotion);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntityRenderers.register(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack mainHand = minecraft.player.getMainHandItem();
        boolean canDash = DashPayload.canDash(mainHand);
        if (!canDash) {
            EpicFightCapabilities.getLocalPlayerPatchAsOptional(minecraft.player)
                    .filter(yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch::isEpicFightMode)
                    .ifPresent(patch -> patch.toVanillaMode(true));
        }
        while (ModKeyMappings.OPEN_INFO.consumeClick()) {
            if (minecraft.screen == null
                    && !minecraft.player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN)) {
                minecraft.setScreen(new PlayerInfoScreen());
            }
        }

        DashInput.tick(minecraft, minecraft.player, mainHand, canDash);
    }
}
