package net.alex.guzhenren.registry.effect;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.MarkerEffect;
import net.alex.guzhenren.effect.pool.LifeQiEffect;
import net.alex.guzhenren.effect.pool.StrengthQiEffect;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CasualThoughtEffect;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.effect.timed.DragonpillCricketGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect;
import net.alex.guzhenren.effect.timed.MaliciousThoughtEffect;
import net.alex.guzhenren.effect.timed.SelfRelianceGuEffect;
import net.alex.guzhenren.effect.timed.TimeRateUpEffect;
import net.alex.guzhenren.effect.timed.VitalityLeafEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every MobEffect this mod registers, one constant per effect.
 *
 * <p>DeferredRegister holder: grades that exclude each other are ONE effect with an amplifier; members
 * meant to be worn TOGETHER get one effect each (the two Watch Gu, so their rates add). Every effect uses
 * the white effect color, including effects granted by Gu materials.
 *
 * <p>⚠ Every effect is built through {@code instance}, which sets
 * {@code showParticles=false, showIcon=true}; the color only feeds the {@link MobEffect} ctor.
 *
 * <p>⚠ A version seam: callers hold an effect as {@code Supplier<? extends MobEffect>} and go through
 * {@link #has}, {@link #get}, {@link #remove}, {@link #instance} and {@link #getEffectOf}, never the loader's handle
 * type, so they stay identical between this NeoForge 1.21.1 mainline ({@code Holder}) and the Forge 1.20.1 port
 * ({@code RegistryObject}, plain {@code MobEffect}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Guzhenren.MOD_ID);
    static final int EFFECT_COLOR = 0xFFFFFF;
    public static final DeferredHolder<MobEffect, VitalityLeafEffect> VITALITY_LEAF = MOB_EFFECTS.register(
            "vitality_leaf", () -> new VitalityLeafEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> LIQUOR_WORM = MOB_EFFECTS.register(
            "liquor_worm", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, LifeQiEffect> LIFE_QI = MOB_EFFECTS.register(
            "life_qi", () -> new LifeQiEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> ESSENCE_QI = MOB_EFFECTS.register(
            "essence_qi", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> DEATH_QI = MOB_EFFECTS.register(
            "death_qi", () -> new MarkerEffect(MobEffectCategory.HARMFUL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, StrengthQiEffect> STRENGTH_QI = MOB_EFFECTS.register(
            "strength_qi", () -> new StrengthQiEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));

    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final DeferredHolder<MobEffect, FlowerBoarGuEffect> FLOWER_BOAR_GU = MOB_EFFECTS.register(
            "flower_boar_gu", () -> new FlowerBoarGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, DragonpillCricketGuEffect> DRAGONPILL_CRICKET_GU =
            MOB_EFFECTS.register("dragonpill_cricket_gu", () -> new DragonpillCricketGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, BruteForceLonghornBeetleGuEffect> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            MOB_EFFECTS.register("brute_force_longhorn_beetle_gu", () -> new BruteForceLonghornBeetleGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Normal [基础力道]
    public static final DeferredHolder<MobEffect, MarkerEffect> ALL_OUT_EFFORT = MOB_EFFECTS.register(
            "all_out_effort", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, CrashGuEffect> HORIZONTAL_CRASH_GU = MOB_EFFECTS.register(
            "horizontal_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, CrashGuEffect> VERTICAL_CRASH_GU = MOB_EFFECTS.register(
            "vertical_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, CrashGuEffect> CHARGING_CRASH_GU =
            MOB_EFFECTS.register("charging_crash_gu", () -> new CrashGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, SelfRelianceGuEffect> SELF_RELIANCE_GU = MOB_EFFECTS.register(
            "self_reliance_gu", () -> new SelfRelianceGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, HardshipStrengthGuEffect> HARDSHIP_STRENGTH_GU =
            MOB_EFFECTS.register("hardship_strength_gu", () -> new HardshipStrengthGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> JADE_SKIN_GU = MOB_EFFECTS.register(
            "jade_skin_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> WHITE_JADE_GU = MOB_EFFECTS.register(
            "white_jade_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    public static final DeferredHolder<MobEffect, MarkerEffect> HALF_ZOMBIE = MOB_EFFECTS.register(
            "half_zombie", () -> new MarkerEffect(MobEffectCategory.NEUTRAL, EFFECT_COLOR));

    //region Watch Gu [更蛊] -- one effect per Gu, so both can be worn at once and their rates add
    public static final DeferredHolder<MobEffect, TimeRateUpEffect> SECOND_WATCH_GU = MOB_EFFECTS.register(
            "second_watch_gu", () -> new TimeRateUpEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 2, 2));
    public static final DeferredHolder<MobEffect, TimeRateUpEffect> THIRD_WATCH_GU = MOB_EFFECTS.register(
            "third_watch_gu", () -> new TimeRateUpEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 3, 3));
    //endregion

    public static final DeferredHolder<MobEffect, MaliciousThoughtEffect> MALICIOUS_THOUGHT_GU = MOB_EFFECTS.register(
            "malicious_thought_gu", () -> new MaliciousThoughtEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR, new long[] { 2L, 20L, 200L, 2_000L }));
    public static final DeferredHolder<MobEffect, CasualThoughtEffect> CASUAL_GU = MOB_EFFECTS.register(
            "casual_gu", () -> new CasualThoughtEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));

    /** Vanilla effects this mod applies, held the same way as its own. */
    public static final Supplier<MobEffect> VANILLA_WEAKNESS = () -> MobEffects.WEAKNESS.value();
    public static final Supplier<MobEffect> VANILLA_CONFUSION = () -> MobEffects.CONFUSION.value();
    public static final Supplier<MobEffect> VANILLA_LEVITATION = () -> MobEffects.LEVITATION.value();
    public static final Supplier<MobEffect> VANILLA_SLOW_FALLING = () -> MobEffects.SLOW_FALLING.value();
    public static final Supplier<MobEffect> VANILLA_ABSORPTION = () -> MobEffects.ABSORPTION.value();

    public static MobEffectInstance instance(@NotNull Supplier<? extends MobEffect> effect, int duration) {
        return new MobEffectInstance(getHolder(effect), duration, 0, false, false, true);
    }

    public static MobEffectInstance instance(@NotNull Supplier<? extends MobEffect> effect, int duration,
                                             int amplifier) {
        return new MobEffectInstance(getHolder(effect), duration, amplifier, false, false, true);
    }

    public static boolean has(@NotNull LivingEntity entity, @NotNull Supplier<? extends MobEffect> effect) {
        return entity.hasEffect(getHolder(effect));
    }

    public static @Nullable MobEffectInstance get(@NotNull LivingEntity entity,
                                                  @NotNull Supplier<? extends MobEffect> effect) {
        return entity.getEffect(getHolder(effect));
    }

    public static boolean remove(@NotNull LivingEntity entity, @NotNull Supplier<? extends MobEffect> effect) {
        return entity.removeEffect(getHolder(effect));
    }

    /** The effect an instance carries; an effect class's own behavior is read off it ({@code instanceof}). */
    public static @NotNull MobEffect getEffectOf(@NotNull MobEffectInstance instance) {
        return instance.getEffect().value();
    }

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect> getHolder(Supplier<? extends MobEffect> effect) {
        if (effect instanceof Holder<?> held) return (Holder<MobEffect>) held;
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
