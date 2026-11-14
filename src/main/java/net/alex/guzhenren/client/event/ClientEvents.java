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
import net.alex.guzhenren.client.input.QiWaveInput;
import net.alex.guzhenren.client.input.StoreStrengthInput;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.renderer.ModEntityRenderers;
import net.alex.guzhenren.client.renderer.SkinFlashLayer;
import net.alex.guzhenren.client.renderer.SkinFlashes;
import net.alex.guzhenren.client.screen.ApertureStorageScreen;
import net.alex.guzhenren.client.screen.BookletScreen;
import net.alex.guzhenren.client.screen.PlayerInfoScreen;
import net.alex.guzhenren.client.screen.RefinementScreen;
import net.alex.guzhenren.client.screen.SoulTradeScreen;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.network.ModPayloads;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
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
 * 本模组全部客户端注册：HUD 层、按键、界面、渲染器。
 *
 * <ul>
 *   <li>九叶生机草[Nine Leaf Vitality Grass]模型属性按客户端世界游戏时间推算叶数。</li>
 *   <li>知心草方块像高草在色调索引 0 取群系草色，上下两半都取下半格；采下的物品无颜色处理器，保留自身 PNG。</li>
 *   <li>客户端初始化时向 {@code ModPayloads} 登记 {@code SkinFlashPayload}、{@code ImpactShakePayload}、
 *       {@code KillingMoveTooltipPayload}、{@code StoreStrengthNoticePayload} 的处理器，{@code ModPayloads} 自身不引用
 *       客户端类；书册阅读器（{@link BookletScreen}）同理登记给 {@code BookletItem}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.ModKeyMappings
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    private static final ResourceLocation PLAYER_STATS = Guzhenren.id("player_stats");
    private static final ResourceLocation CHARGE = Guzhenren.id("charge");
    private static final ResourceLocation NOURISH = Guzhenren.id("nourish");
    private static final ResourceLocation SHIELD = Guzhenren.id("shield");
    private static final ResourceLocation SHIELD_GLOW = Guzhenren.id("shield_glow");
    private static final ResourceLocation SONIC_BOOM = Guzhenren.id("sonic_boom");
    private static final ResourceLocation KILLING_MOVE_TOOLTIP = Guzhenren.id("killing_move_tooltip");
    private static final ResourceLocation RADIAL_MENU = Guzhenren.id("radial_menu");
    private static final ResourceLocation HOLD_SLIDER = Guzhenren.id("hold_slider");
    private static final ResourceLocation AWAKENING = Guzhenren.id("awakening");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SONIC_BOOM, SonicBoomHud.INSTANCE::render);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SHIELD_GLOW, ShieldGlowHud.INSTANCE::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, PLAYER_STATS, (g, delta) -> PlayerStatsHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.HOTBAR, KILLING_MOVE_TOOLTIP,
                (g, delta) -> KillingMoveTooltipHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, CHARGE, (g, delta) -> ChargeHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, NOURISH, (g, delta) -> NourishHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, SHIELD, (g, delta) -> ShieldHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, RADIAL_MENU, (g, delta) -> RadialMenu.render(g));
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, HOLD_SLIDER, (g, delta) -> HoldSlider.render(g));
        event.registerAboveAll(AWAKENING, AwakeningHud.INSTANCE::render);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.PURE_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_PURE_WATER.get(), RenderType.translucent());
            ItemProperties.register(ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), NineLeafVitalityGrassItem.PICKED,
                    (ClampedItemPropertyFunction) ClientEvents::getPickedStage);
            ModPayloads.handleOnClient(SkinFlashPayload.class, SkinFlashes::flash);
            ModPayloads.handleOnClient(ImpactShakePayload.class, ImpactShakes::onPayload);
            ModPayloads.handleOnClient(KillingMoveTooltipPayload.class, KillingMoveTooltipHud.INSTANCE::show);
            ModPayloads.handleOnClient(StoreStrengthNoticePayload.class, StoreStrengthInput::onNotice);
            BookletItem.readOnClient(BookletScreen::open);
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
        event.registerSpriteSet(ModParticles.SONIC_BOOM_RING.get(), RingParticle::createSonicBoom);
        event.registerSpriteSet(ModParticles.BARRIER_RING.get(), RingParticle::createBarrierRing);
        event.registerSpriteSet(ModParticles.AREA_IMPACT_RING.get(), RingParticle::createAreaImpactRing);
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
        QiWaveInput.reset();
        StoreStrengthInput.reset();
        SkinFlashes.clear();
        ShieldGlowHud.INSTANCE.reset();
        KillingMoveTooltipHud.INSTANCE.clear();
        RadialMenu.close();
        HoldSlider.close();
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
        QiWaveInput.tick(minecraft, minecraft.player);
        StoreStrengthInput.tick(minecraft, minecraft.player);
    }
}
