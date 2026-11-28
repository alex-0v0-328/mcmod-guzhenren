package net.alex.guzhenren.client.event;

import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.alex.guzhenren.client.hud.AwakeningHud;
import net.alex.guzhenren.client.hud.ChargeHud;
import net.alex.guzhenren.client.hud.CinematicHud;
import net.alex.guzhenren.client.hud.KillingMoveTooltipHud;
import net.alex.guzhenren.client.hud.NourishHud;
import net.alex.guzhenren.client.hud.PlaceTitleHud;
import net.alex.guzhenren.client.hud.PlayerStatsHud;
import net.alex.guzhenren.client.hud.ShieldGlowHud;
import net.alex.guzhenren.client.hud.ShieldHud;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.client.input.DashInput;
import net.alex.guzhenren.client.input.QiWaveInput;
import net.alex.guzhenren.client.input.StoreStrengthInput;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.renderer.ModEntityRenderers;
import net.alex.guzhenren.client.renderer.SkinFlashLayer;
import net.alex.guzhenren.client.renderer.SkinFlashes;
import net.alex.guzhenren.client.renderer.armor.SchoolUniformArmorRenderer;
import net.alex.guzhenren.client.screen.ApertureStorageScreen;
import net.alex.guzhenren.client.screen.BookletScreen;
import net.alex.guzhenren.client.screen.KnownRecipes;
import net.alex.guzhenren.client.screen.PlayerInfoScreen;
import net.alex.guzhenren.client.screen.RefinementScreen;
import net.alex.guzhenren.client.screen.SoulTradeScreen;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.compat.TravelersTitlesIntegration;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
import net.alex.guzhenren.item.armor.UniformColor;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.network.ModPayloads;
import net.alex.guzhenren.network.payload.EarthSpiritPanelPayload;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.PlaceTitlePayload;
import net.alex.guzhenren.network.payload.SkinFlashPayload;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 本模组客户端总注册入口：统一注册 HUD 图层、按键绑定、菜单界面与实体渲染器。
 * <ul>
 *   <li>九叶生机草[Nine Leaf Vitality Grass]的物品模型属性依据客户端世界游戏时间推算当前叶数。</li>
 *   <li>客户端初始化时向 {@code ModPayloads} 注册 {@code SkinFlashPayload}、{@code ImpactShakePayload}、
 *       {@code KillingMoveTooltipPayload}、{@code StoreStrengthNoticePayload} 与 {@code PlaceTitlePayload} 的处理器，使
 *       {@code ModPayloads} 本身不直接依赖客户端类；书册阅读器（{@link BookletScreen}）同理注册给 {@code BookletItem}。
 *       </li>
 *   <li>温养[Nourish]运镜显示期间（{@link CultivationCamera#isShowing}）仅保留 {@link #CINEMATIC_LAYERS} 白名单图层：上
 *       下黑边、温养条、扇形选择器、长按滑条、调试覆盖层、Tab 列表、字幕、动作栏提示与存档提示，聊天栏仅在打开聊天界面
 *       时绘制。</li>
 *   <li>地名标题显示期间取消 Traveler's Titles 的图层，两者不同时出字（排队见 {@link PlaceTitleHud}）。</li>
 * </ul>
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
    private static final ResourceLocation CINEMATIC = Guzhenren.id("cinematic");
    private static final ResourceLocation PLACE_TITLE = Guzhenren.id("place_title");
    private static final Set<ResourceLocation> CINEMATIC_LAYERS = Set.of(CINEMATIC, NOURISH, RADIAL_MENU, HOLD_SLIDER,
            VanillaGuiLayers.DEBUG_OVERLAY, VanillaGuiLayers.TAB_LIST, VanillaGuiLayers.SUBTITLE_OVERLAY,
            VanillaGuiLayers.OVERLAY_MESSAGE, VanillaGuiLayers.SAVING_INDICATOR);

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(CINEMATIC, CinematicHud.INSTANCE::render);
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
        event.registerAbove(VanillaGuiLayers.TITLE, PLACE_TITLE, PlaceTitleHud.INSTANCE::render);
        event.registerAboveAll(AWAKENING, AwakeningHud.INSTANCE::render);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (PlaceTitleHud.INSTANCE.isShowing() && event.getName().equals(TravelersTitlesIntegration.LAYER)) {
            event.setCanceled(true);
            return;
        }
        if (!CultivationCamera.isShowing() || CINEMATIC_LAYERS.contains(event.getName())) return;
        boolean chatting = Minecraft.getInstance().screen instanceof ChatScreen;
        if (chatting && event.getName().equals(VanillaGuiLayers.CHAT)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.PURE_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_PURE_WATER.get(), RenderType.translucent());
            ItemProperties.register(ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), NineLeafVitalityGrassItem.PICKED,
                    (ClampedItemPropertyFunction) (stack, level, entity, seed) -> getPickedStage(stack, level, entity));
            ModPayloads.handleOnClient(SkinFlashPayload.class, SkinFlashes::flash);
            ModPayloads.handleOnClient(ImpactShakePayload.class, ImpactShakes::onPayload);
            ModPayloads.handleOnClient(KillingMoveTooltipPayload.class, KillingMoveTooltipHud.INSTANCE::show);
            ModPayloads.handleOnClient(StoreStrengthNoticePayload.class, StoreStrengthInput::onNotice);
            ModPayloads.handleOnClient(PlaceTitlePayload.class, PlaceTitleHud.INSTANCE::show);
            ModPayloads.handleOnClient(EarthSpiritPanelPayload.class, EarthSpiritScreen::onPanel);
            BookletItem.readOnClient(BookletScreen::open);
            GuRecipeItem.readOnClient(BookletScreen::openRecipe);
            GuRecipeItem.lookUpOnClient(KnownRecipes::find, KnownRecipes::getIds);
            SchoolUniformItem.renderOnClient(() -> new GeoRenderProvider() {
                private @Nullable GeoArmorRenderer<?> renderer;

                @Override
                public <T extends LivingEntity> @NotNull HumanoidModel<?> getGeoArmorRenderer(
                        @Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot,
                        @Nullable HumanoidModel<T> original) {
                    if (this.renderer == null) this.renderer = new SchoolUniformArmorRenderer();
                    return this.renderer;
                }
            });
            EpicFightClientIntegration.initialize();
        });
    }

    private static float getPickedStage(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        if (!(stack.getItem() instanceof NineLeafVitalityGrassItem grass)) return 0.0F;
        if (level != null) return grass.getPickedStage(stack, level.getGameTime());
        if (entity != null) return grass.getPickedStage(stack, entity.level().getGameTime());
        return Minecraft.getInstance().level != null
                ? grass.getPickedStage(stack, Minecraft.getInstance().level.getGameTime())
                : 0.0F;
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public int getArmorLayerTintColor(@NotNull ItemStack stack, @NotNull LivingEntity entity,
                                              ArmorMaterial.@NotNull Layer layer, int layerIdx, int fallbackColor) {
                if (!layer.dyeable()) return 0xFFFFFFFF;
                if (stack.getItem() instanceof SchoolUniformItem uniform && uniform.isDyeable()) {
                    return UniformColor.argb(UniformColor.getColor(stack));
                }
                return 0;
            }
        }, ModItems.SCHOOL_UNIFORM_CHESTPLATE.get(), ModItems.SCHOOL_UNIFORM_LEGGINGS.get(),
                ModItems.SCHOOL_UNIFORM_BOOTS.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? UniformColor.argb(UniformColor.getColor(stack)) : -1,
                ModItems.SCHOOL_UNIFORM_CHESTPLATE.get(), ModItems.SCHOOL_UNIFORM_LEGGINGS.get());
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
        event.register(ModMenus.EARTH_SPIRIT_MENU.get(), EarthSpiritScreen.factory());
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), RingParticle::createDashTrail);
        event.registerSpriteSet(ModParticles.SONIC_BOOM_RING.get(), RingParticle::createSonicBoom);
        event.registerSpriteSet(ModParticles.BARRIER_RING.get(), RingParticle::createBarrierRing);
        event.registerSpriteSet(ModParticles.AREA_DAMAGE_RING.get(), RingParticle::createAreaDamageRing);
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
