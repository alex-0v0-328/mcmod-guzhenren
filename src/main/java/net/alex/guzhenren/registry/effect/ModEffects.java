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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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
 * {@link #has}, {@link #get}, {@link #remove}, {@link #instance} and {@link #effectOf}, never the loader's handle
 * type, so they stay identical between the NeoForge 1.21.1 mainline ({@code Holder}) and this Forge 1.20.1 port
 * ({@code RegistryObject}, plain {@code MobEffect}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Guzhenren.MOD_ID);
    static final int EFFECT_COLOR = 0xFFFFFF;
    public static final RegistryObject<VitalityLeafEffect> VITALITY_LEAF = MOB_EFFECTS.register(
            "vitality_leaf", () -> new VitalityLeafEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<MarkerEffect> LIQUOR_WORM = MOB_EFFECTS.register(
            "liquor_worm", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<LifeQiEffect> LIFE_QI = MOB_EFFECTS.register(
            "life_qi", () -> new LifeQiEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<MarkerEffect> ESSENCE_QI = MOB_EFFECTS.register(
            "essence_qi", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<MarkerEffect> DEATH_QI = MOB_EFFECTS.register(
            "death_qi", () -> new MarkerEffect(MobEffectCategory.HARMFUL, EFFECT_COLOR));
    public static final RegistryObject<StrengthQiEffect> STRENGTH_QI = MOB_EFFECTS.register(
            "strength_qi", () -> new StrengthQiEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));

    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final RegistryObject<FlowerBoarGuEffect> FLOWER_BOAR_GU = MOB_EFFECTS.register(
            "flower_boar_gu", () -> new FlowerBoarGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<DragonpillCricketGuEffect> DRAGONPILL_CRICKET_GU =
            MOB_EFFECTS.register("dragonpill_cricket_gu", () -> new DragonpillCricketGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<BruteForceLonghornBeetleGuEffect> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            MOB_EFFECTS.register("brute_force_longhorn_beetle_gu", () -> new BruteForceLonghornBeetleGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Normal [基础力道]
    public static final RegistryObject<MarkerEffect> ALL_OUT_EFFORT = MOB_EFFECTS.register(
            "all_out_effort", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<CrashGuEffect> HORIZONTAL_CRASH_GU = MOB_EFFECTS.register(
            "horizontal_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<CrashGuEffect> VERTICAL_CRASH_GU = MOB_EFFECTS.register(
            "vertical_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<CrashGuEffect> CHARGING_CRASH_GU =
            MOB_EFFECTS.register("charging_crash_gu", () -> new CrashGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<SelfRelianceGuEffect> SELF_RELIANCE_GU = MOB_EFFECTS.register(
            "self_reliance_gu", () -> new SelfRelianceGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final RegistryObject<HardshipStrengthGuEffect> HARDSHIP_STRENGTH_GU =
            MOB_EFFECTS.register("hardship_strength_gu", () -> new HardshipStrengthGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    public static final RegistryObject<MarkerEffect> HALF_ZOMBIE = MOB_EFFECTS.register(
            "half_zombie", () -> new MarkerEffect(MobEffectCategory.NEUTRAL, EFFECT_COLOR));

    //region Watch Gu [更蛊] -- one effect per Gu, so both can be worn at once and their rates add
    public static final RegistryObject<TimeRateUpEffect> SECOND_WATCH_GU = MOB_EFFECTS.register(
            "second_watch_gu", () -> new TimeRateUpEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 2, 2));
    public static final RegistryObject<TimeRateUpEffect> THIRD_WATCH_GU = MOB_EFFECTS.register(
            "third_watch_gu", () -> new TimeRateUpEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 3, 3));
    //endregion

    public static final RegistryObject<MaliciousThoughtEffect> MALICIOUS_THOUGHT_GU = MOB_EFFECTS.register(
            "malicious_thought_gu", () -> new MaliciousThoughtEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR, new long[] { 2L, 20L, 200L, 2_000L }));
    public static final RegistryObject<CasualThoughtEffect> CASUAL_GU = MOB_EFFECTS.register(
            "casual_gu", () -> new CasualThoughtEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));

    /** Vanilla effects this mod applies, held the same way as its own. */
    public static final Supplier<MobEffect> VANILLA_WEAKNESS = () -> MobEffects.WEAKNESS;
    public static final Supplier<MobEffect> VANILLA_CONFUSION = () -> MobEffects.CONFUSION;

    public static MobEffectInstance instance(@NotNull Supplier<? extends MobEffect> effect, int duration) {
        return new MobEffectInstance(effect.get(), duration, 0, false, false, true);
    }

    public static MobEffectInstance instance(@NotNull Supplier<? extends MobEffect> effect, int duration,
                                             int amplifier) {
        return new MobEffectInstance(effect.get(), duration, amplifier, false, false, true);
    }

    public static boolean has(@NotNull LivingEntity entity, @NotNull Supplier<? extends MobEffect> effect) {
        return entity.hasEffect(effect.get());
    }

    public static @Nullable MobEffectInstance get(@NotNull LivingEntity entity,
                                                  @NotNull Supplier<? extends MobEffect> effect) {
        return entity.getEffect(effect.get());
    }

    public static boolean remove(@NotNull LivingEntity entity, @NotNull Supplier<? extends MobEffect> effect) {
        return entity.removeEffect(effect.get());
    }

    /** The effect an instance carries; an effect class's own behavior is read off it ({@code instanceof}). */
    public static @NotNull MobEffect effectOf(@NotNull MobEffectInstance instance) { return instance.getEffect(); }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
