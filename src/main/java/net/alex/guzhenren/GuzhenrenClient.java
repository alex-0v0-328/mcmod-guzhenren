package net.alex.guzhenren;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.alex.guzhenren.client.icon.GradedEffectIcon;
import net.alex.guzhenren.client.icon.ItemEffectIcon;
import net.alex.guzhenren.registry.effect.ModEffects;
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
 * effect icons; all other client-side event subscribers live under the {@code client/} package tree and are loaded
 * only on the client. The Spirit Spring's fluid rendering is handed out by its fluid type in
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
        registerMobEffect(GradedEffectIcon.mobEffect("essence_qi", 1, 5), ModEffects.ESSENCE_QI);
        registerMobEffect(GradedEffectIcon.mobEffect("life_qi", 1, 5), ModEffects.LIFE_QI);
        registerMobEffect(GradedEffectIcon.mobEffect("strength_qi", 1, 5), ModEffects.STRENGTH_QI);
        registerMobEffect(GradedEffectIcon.mobEffect("all_out_effort", 3, 5), ModEffects.ALL_OUT_EFFORT);
        registerMobEffect(GradedEffectIcon.mobEffect("liquor_worm", 1, 4), ModEffects.LIQUOR_WORM);
        registerMobEffect(GradedEffectIcon.item("casual_gu", 1, 2), ModEffects.CASUAL_GU);
        registerMobEffect(GradedEffectIcon.item("malicious_thought_gu", 2, 5, 2), ModEffects.MALICIOUS_THOUGHT_GU);
        registerMobEffect(GradedEffectIcon.item("self_reliance_gu", 2, 4), ModEffects.SELF_RELIANCE_GU);
        registerMobEffect(new ItemEffectIcon("hardship_strength_gu"), ModEffects.HARDSHIP_STRENGTH_GU);
        registerMobEffect(new ItemEffectIcon("horizontal_crash_gu"), ModEffects.HORIZONTAL_CRASH_GU);
        registerMobEffect(new ItemEffectIcon("vertical_crash_gu"), ModEffects.VERTICAL_CRASH_GU);
        registerMobEffect(GradedEffectIcon.item("charging_crash_gu", 4, 5), ModEffects.CHARGING_CRASH_GU);
        registerMobEffect(new ItemEffectIcon("second_watch_gu"), ModEffects.SECOND_WATCH_GU);
        registerMobEffect(new ItemEffectIcon("third_watch_gu"), ModEffects.THIRD_WATCH_GU);
    }

    private static void registerMobEffect(IClientMobEffectExtensions extensions,
                                          Supplier<? extends MobEffect> effect) {
        MOB_EFFECT_EXTENSIONS.put(effect.get(), extensions);
    }
}
