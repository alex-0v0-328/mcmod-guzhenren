package net.alex.guzhenren;

import net.alex.guzhenren.client.fluid.PureWaterClientExtensions;
import net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions;
import net.alex.guzhenren.client.icon.EffectIcons;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * 仅客户端的入口。
 *
 * <ul>
 *   <li>会崩专用服务端的内容只放这里，不进 {@link Guzhenren}；其余客户端订阅者在 {@code client/} 包下，仅客户端加
 *       载。</li>
 *   <li>模组总线上注册 MobEffect 客户端扩展（{@link EffectIcons} 效果图标）与元泉[Spirit Spring]、纯水流体渲染。</li>
 *   <li>宝黄天[Treasure Yellow Heaven]的天空效果归姊妹模组 Gu World。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Guzhenren
 * @since 1.0.0
 */

@Mod(value = Guzhenren.MOD_ID, dist = Dist.CLIENT)
public class GuzhenrenClient {

    public GuzhenrenClient(IEventBus modEventBus) {
        modEventBus.addListener(GuzhenrenClient::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        EffectIcons.forEach((icon, effect) -> event.registerMobEffect(icon, effect.get()));
        event.registerFluidType(new SpiritSpringClientExtensions(), ModFluidTypes.SPIRIT_SPRING.get());
        event.registerFluidType(new PureWaterClientExtensions(), ModFluidTypes.PURE_WATER.get());
    }
}
