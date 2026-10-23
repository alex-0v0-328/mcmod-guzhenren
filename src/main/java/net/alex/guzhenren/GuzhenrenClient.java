package net.alex.guzhenren;

import java.util.IdentityHashMap;
import java.util.Map;
import net.alex.guzhenren.client.icon.EffectIcons;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only entry point, so that nothing which would crash a dedicated server sits in {@link Guzhenren}.
 *
 * <p>Annotated {@code @Mod.EventBusSubscriber(Dist.CLIENT)}. It keeps every MobEffect's client extension -- the
 * effect icons of {@link EffectIcons}; all other client-side event subscribers live under the {@code client/} package
 * tree and are loaded only on the client. The Spirit Spring's fluid rendering is handed out by its fluid type in
 * {@code ModFluidTypes}. The Treasure Yellow Heaven's sky effects belong to the sibling mod Gu World.
 *
 * <p>⚠ Loader glue: the NeoForge 1.21.1 mainline registers the icons in a client extension event. Forge 1.20.1
 * reads a {@code MobEffect}'s extensions from a private field that only {@code MobEffect#initializeClient} fills,
 * from inside the {@code MobEffect} constructor, so a registration cannot reach it: {@code MobEffectMixin} answers
 * {@code MobEffect#getEffectRendererInternal}, which {@link IClientMobEffectExtensions#of} reads, from
 * {@link #getMobEffectExtensions}.
 *
 * @author Alex
 * @version 1.0.0
 * @see Guzhenren
 * @see net.alex.guzhenren.mixin.MobEffectMixin
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GuzhenrenClient {

    private GuzhenrenClient() {}

    private static final Map<MobEffect, IClientMobEffectExtensions> MOB_EFFECT_EXTENSIONS = new IdentityHashMap<>();

    public static @Nullable IClientMobEffectExtensions getMobEffectExtensions(@NotNull MobEffect effect) {
        return MOB_EFFECT_EXTENSIONS.get(effect);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(GuzhenrenClient::registerMobEffectExtensions);
    }

    private static void registerMobEffectExtensions() {
        EffectIcons.forEach((icon, effect) -> MOB_EFFECT_EXTENSIONS.put(effect.get(), icon));
    }
}
