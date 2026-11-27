package net.alex.guzhenren;

import net.alex.guzhenren.client.fluid.PureWaterClientExtensions;
import net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions;
import net.alex.guzhenren.client.icon.EffectIcons;
import net.alex.guzhenren.client.renderer.HumanSoulGeoRenderer;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.compat.FtbLibraryIntegration;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * 主模组的客户端专属入口类（{@code Dist.CLIENT}）。
 * <ul>
 *   <li>集中放置直接引用客户端类而会导致专用服务端崩溃的模组总线初始化逻辑，与双端入口 {@link Guzhenren} 隔离；其余客户
 *       端事件订阅者则分布在 {@code client/} 包下。</li>
 *   <li>在 {@link RegisterClientExtensionsEvent} 中统一注册状态效果的自定义图标渲染扩展（{@link EffectIcons}），
 *       以及元泉[Spirit Spring]与纯水两种自定义流体的客户端材质与雾气扩展；宝黄天[Treasure Yellow Heaven]的维度天空效果
 *       则归属配套模组 Gu World。</li>
 *   <li>人魂身体与眼部发光的固定渲染缓冲（{@link HumanSoulGeoRenderer#registerBuffers}）；客户端初始化时把地灵面板两个
 *       界面类排除出 FTB Library 侧栏（{@link FtbLibraryIntegration}）。</li>
 * </ul>
 */

@Mod(value = Guzhenren.MOD_ID, dist = Dist.CLIENT)
public class GuzhenrenClient {

    public GuzhenrenClient(IEventBus modEventBus) {
        modEventBus.addListener(GuzhenrenClient::onRegisterClientExtensions);
        modEventBus.addListener(RegisterRenderBuffersEvent.class, HumanSoulGeoRenderer::registerBuffers);
        modEventBus.addListener(GuzhenrenClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> FtbLibraryIntegration.hideSidebarOn(EarthSpiritScreen.Pages.class,
                EarthSpiritScreen.Storage.class));
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        EffectIcons.forEach((icon, effect) -> event.registerMobEffect(icon, effect.get()));
        event.registerFluidType(new SpiritSpringClientExtensions(), ModFluidTypes.SPIRIT_SPRING.get());
        event.registerFluidType(new PureWaterClientExtensions(), ModFluidTypes.PURE_WATER.get());
    }
}
