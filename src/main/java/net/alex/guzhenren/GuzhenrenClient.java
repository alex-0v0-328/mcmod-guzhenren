package net.alex.guzhenren;

import net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions;
import net.alex.guzhenren.client.icon.EffectIcons;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client-only entry point, so that nothing which would crash a dedicated server sits in {@link Guzhenren}.
 *
 * <p>Annotated {@code @Mod(dist = Dist.CLIENT)}. It registers every MobEffect's client extension --
 * the effect icons of {@link EffectIcons} -- plus the Spirit Spring's fluid rendering on the mod bus; all other
 * client-side event subscribers live under the {@code client/} package tree and are loaded only on the client.
 * The Treasure Yellow Heaven's sky effects belong to the sibling mod Gu World.
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
    }
}
