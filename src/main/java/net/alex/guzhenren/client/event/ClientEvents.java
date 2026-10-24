package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.hud.ChargeHud;
import net.alex.guzhenren.client.hud.NourishHud;
import net.alex.guzhenren.client.hud.PlayerStatsHud;
import net.alex.guzhenren.client.input.DashInput;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.client.screen.ApertureStorageScreen;
import net.alex.guzhenren.client.screen.PlayerInfoScreen;
import net.alex.guzhenren.client.screen.RefinementScreen;
import net.alex.guzhenren.client.screen.SoulTradeScreen;
import net.alex.guzhenren.item.gu.mortal.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Every client-side registration this mod makes: HUD overlays, key mappings, screens and renderers.
 *
 * <p>Annotated {@code @Mod.EventBusSubscriber(Dist.CLIENT)} on the mod bus, with {@link GameBus} for the two Forge
 * bus events. Registers three GUI overlays ({@link net.alex.guzhenren.client.hud.PlayerStatsHud},
 * {@link net.alex.guzhenren.client.hud.ChargeHud},
 * {@link net.alex.guzhenren.client.hud.NourishHud}), the key mapping for the B panel, the menu
 * screens for the three containers, the shockwave-ring particle provider, the Spirit Spring fluid's
 * translucent render layer, and the Nine Leaf Vitality
 * Grass [九叶生机草] model property, which derives the leaf count from the client level's game time. The Intimate
 * Grass [知心草] block takes the biome grass color on its tint index 0 the way vanilla tall grass does, sampled at
 * the lower half for both halves; its harvested item has no color handler and keeps its own PNG. On logout it
 * clears {@link DashInput}'s remembered key states. The entity renderers register themselves in
 * {@link net.alex.guzhenren.client.renderer.ModEntityRenderers}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.ModKeyMappings
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    private static final String PLAYER_STATS = "player_stats";
    private static final String CHARGE = "charge";
    private static final String NOURISH = "nourish";

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), PLAYER_STATS,
                (gui, g, partialTick, width, height) -> PlayerStatsHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), CHARGE,
                (gui, g, partialTick, width, height) -> ChargeHud.INSTANCE.render(g));
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), NOURISH,
                (gui, g, partialTick, width, height) -> NourishHud.INSTANCE.render(g));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
            ItemProperties.register(ModItems.NINE_LEAF_VITALITY_GRASS.get(), NineLeafVitalityGrassItem.PICKED,
                    (ClampedItemPropertyFunction) ClientEvents::getPickedStage);
            MenuScreens.register(ModMenus.APERTURE_STORAGE_MENU.get(), ApertureStorageScreen::new);
            MenuScreens.register(ModMenus.REFINEMENT_MENU.get(), RefinementScreen::new);
            MenuScreens.register(ModMenus.SOUL_TRADE_MENU.get(), SoulTradeScreen::new);
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
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), RingParticle::createDashTrail);
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), RingParticle::createFacingMotion);
    }

    private static void tickDash() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack mainHand = minecraft.player.getMainHandItem();
        boolean canDash = ClientIntents.canDash(mainHand);
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

    /** The events Forge fires on its game bus rather than the mod bus; a subscriber class takes one bus. */
    @Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {

        private GameBus() {}

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { DashInput.reset(); }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) tickDash();
        }
    }
}
