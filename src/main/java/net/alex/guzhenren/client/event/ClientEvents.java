package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.hud.AwakeningHud;
import net.alex.guzhenren.client.hud.ChargeHud;
import net.alex.guzhenren.client.hud.KillingMoveTooltipHud;
import net.alex.guzhenren.client.hud.ShieldGlowHud;
import net.alex.guzhenren.client.hud.ShieldHud;
import net.alex.guzhenren.client.hud.NourishHud;
import net.alex.guzhenren.client.hud.PlayerStatsHud;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.client.input.DashInput;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.renderer.ModEntityRenderers;
import net.alex.guzhenren.client.renderer.SkinFlashLayer;
import net.alex.guzhenren.client.renderer.SkinFlashes;
import net.alex.guzhenren.client.screen.ApertureStorageScreen;
import net.alex.guzhenren.client.screen.PlayerInfoScreen;
import net.alex.guzhenren.client.screen.RefinementScreen;
import net.alex.guzhenren.client.screen.SoulTradeScreen;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.item.gu.mortal.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.network.ModPayloads;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

/**
 * Every client-side registration this mod makes: HUD layers, key mappings, screens and renderers.
 *
 * <p>Annotated {@code @EventBusSubscriber(Dist.CLIENT)}. Registers nine GUI layers
 * ({@link net.alex.guzhenren.client.hud.SonicBoomHud} and {@link ShieldGlowHud} above the vanilla camera overlays,
 * {@link net.alex.guzhenren.client.hud.PlayerStatsHud} and {@link KillingMoveTooltipHud} above the hotbar,
 * {@link net.alex.guzhenren.client.hud.ChargeHud},
 * {@link net.alex.guzhenren.client.hud.NourishHud}, {@link ShieldHud}, {@link RadialMenu} above the crosshair, and
 * {@link AwakeningHud} above everything),
 * the key mapping for
 * the B panel, the menu screens for the three containers, the shockwave-ring particle providers, the entity renderers
 * ({@link ModEntityRenderers}), the Spirit Spring and Pure Water fluids' translucent render layer, and the Nine Leaf
 * Vitality Grass [九叶生机草] model property, which derives the leaf count from the client level's game time. The Intimate
 * Grass [知心草] block takes the biome grass color on its tint index 0 the way vanilla tall grass does, sampled at the
 * lower half for both halves; its harvested item has no color handler and keeps its own PNG. Both player renderers get
 * a {@link SkinFlashLayer}, and client setup registers the {@code SkinFlashPayload}, {@code ImpactShakePayload} and
 * {@code KillingMoveTooltipPayload} handlers with {@code ModPayloads}, which names no client class itself, and the
 * Epic Fight client hooks ({@link EpicFightClientIntegration}). On logout it clears {@link DashInput}'s remembered
 * key states, the pending flashes, the shield glow and the Killing Move Tooltip, and drops a radial-menu hold; each
 * client tick steps the shield glow.
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
    private static final ResourceLocation SHIELD =
            Guzhenren.id("shield");
    private static final ResourceLocation SHIELD_GLOW =
            Guzhenren.id("shield_glow");
    private static final ResourceLocation SONIC_BOOM =
            Guzhenren.id("sonic_boom");
    private static final ResourceLocation KILLING_MOVE_TOOLTIP =
            Guzhenren.id("killing_move_tooltip");
    private static final ResourceLocation RADIAL_MENU =
            Guzhenren.id("radial_menu");
    private static final ResourceLocation AWAKENING =
            Guzhenren.id("awakening");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SONIC_BOOM,
                (g, delta) -> SonicBoomHud.INSTANCE.render(g, delta));
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SHIELD_GLOW,
                (g, delta) -> ShieldGlowHud.INSTANCE.render(g, delta));
        event.registerAbove(VanillaGuiLayers.HOTBAR, PLAYER_STATS, (g, delta) -> PlayerStatsHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.HOTBAR, KILLING_MOVE_TOOLTIP,
                (g, delta) -> KillingMoveTooltipHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, CHARGE, (g, delta) -> ChargeHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, NOURISH, (g, delta) -> NourishHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, SHIELD, (g, delta) -> ShieldHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, RADIAL_MENU, (g, delta) -> RadialMenu.render(g));
        event.registerAboveAll(AWAKENING, (g, delta) -> AwakeningHud.INSTANCE.render(g, delta));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.PURE_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_PURE_WATER.get(), RenderType.translucent());
            ItemProperties.register(ModItems.NINE_LEAF_VITALITY_GRASS.get(), NineLeafVitalityGrassItem.PICKED,
                    (ClampedItemPropertyFunction) ClientEvents::getPickedStage);
            ModPayloads.handleOnClient(SkinFlashPayload.class, SkinFlashes::flash);
            ModPayloads.handleOnClient(ImpactShakePayload.class, ImpactShakes::onPayload);
            ModPayloads.handleOnClient(KillingMoveTooltipPayload.class, KillingMoveTooltipHud.INSTANCE::show);
            EpicFightClientIntegration.initialize();
        });
    }

    private static float getPickedStage(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity,
                                        int seed) {
        if (!(stack.getItem() instanceof NineLeafVitalityGrassItem grass)) return 0.0F;
        if (level != null) return grass.getPickedStage(stack, level.getGameTime());
        if (entity != null) return grass.getPickedStage(stack, entity.level().getGameTime());
        return Minecraft.getInstance().level != null
                ? grass.getPickedStage(stack, Minecraft.getInstance().level.getGameTime())
                : 0.0F;
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(ClientEvents::getGrassColor, ModBlocks.INTIMATE_GRASS.get());
    }

    private static int getGrassColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                                     int tintIndex) {
        if (level == null || pos == null) return GrassColor.getDefaultColor();
        boolean upper = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
        return BiomeColors.getAverageGrassColor(level, upper ? pos.below() : pos);
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
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), RingParticle::createDashTrail);
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), RingParticle::createFacingMotion);
        event.registerSpriteSet(ModParticles.SONIC_BOOM_RING.get(), RingParticle::createSonicBoom);
        event.registerSpriteSet(ModParticles.BARRIER_RING.get(), RingParticle::createBarrierRing);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntityRenderers.register(event);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new SkinFlashLayer(renderer, skin == PlayerSkin.Model.SLIM));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DashInput.reset();
        SkinFlashes.clear();
        ShieldGlowHud.INSTANCE.reset();
        KillingMoveTooltipHud.INSTANCE.clear();
        RadialMenu.close();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SkinFlashes.prune(minecraft.player.level().getGameTime());
        ShieldGlowHud.INSTANCE.tick(minecraft.player);
        while (ModKeyMappings.OPEN_INFO.consumeClick()) {
            if (minecraft.screen == null
                    && !minecraft.player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN)) {
                minecraft.setScreen(new PlayerInfoScreen());
            }
        }

        DashInput.tick(minecraft, minecraft.player);
    }
}
