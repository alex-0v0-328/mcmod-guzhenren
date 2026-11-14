package net.alex.guzhenren.registry.effect;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.BleedingEffect;
import net.alex.guzhenren.effect.MarkerEffect;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CasualGuEffect;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect;
import net.alex.guzhenren.effect.timed.MaliciousThoughtGuEffect;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.effect.timed.WatchGuEffect;
import net.alex.guzhenren.effect.timed.VitalityLeafGuEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组注册的全部 MobEffect。
 *
 * <ul>
 *   <li>互斥档次合为一个效果，以 amplifier 区分；须同戴的各占一个（两更蛊[Second Watch Gu]、三更蛊[Third Watch Gu]，
 *       倍率相加）。</li>
 *   <li>全部用白色效果色，含蛊材给的效果；region 顺序同 {@code GuPath}、{@code ModItems}。</li>
 *   <li>⚠ 实例经 {@code instance}：{@code showParticles=false, showIcon=true}；颜色只给 {@link MobEffect} 构造。</li>
 *   <li>调用方持 {@code Holder<MobEffect>}（自家 {@code DeferredHolder} 或原版 {@code MobEffects}），直接查实体。</li>
 * </ul>
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
    //region Time Path [宙道]
    public static final DeferredHolder<MobEffect, WatchGuEffect> SECOND_WATCH_GU = MOB_EFFECTS.register(
            "second_watch_gu", () -> new WatchGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 2, 2));
    public static final DeferredHolder<MobEffect, WatchGuEffect> THIRD_WATCH_GU = MOB_EFFECTS.register(
            "third_watch_gu", () -> new WatchGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR, 3, 3));
    //endregion

    //region Wood Path [木道]
    public static final DeferredHolder<MobEffect, VitalityLeafGuEffect> VITALITY_LEAF_GU = MOB_EFFECTS.register(
            "vitality_leaf_gu", () -> new VitalityLeafGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Water Path [水道]
    public static final DeferredHolder<MobEffect, MarkerEffect> HEAVENLY_MUGWORT_GU = MOB_EFFECTS.register(
            "heavenly_mugwort_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Earth Path [土道]
    public static final DeferredHolder<MobEffect, MarkerEffect> JADE_SKIN_GU = MOB_EFFECTS.register(
            "jade_skin_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> WHITE_JADE_GU = MOB_EFFECTS.register(
            "white_jade_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Qi Path [气道]
    public static final DeferredHolder<MobEffect, StrengthQiGuEffect> STRENGTH_QI_GU = MOB_EFFECTS.register(
            "strength_qi_gu", () -> new StrengthQiGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Strength Path [力道]
    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final DeferredHolder<MobEffect, FlowerBoarGuEffect> FLOWER_BOAR_GU = MOB_EFFECTS.register(
            "flower_boar_gu", () -> new FlowerBoarGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, BruteForceLonghornBeetleGuEffect> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            MOB_EFFECTS.register("brute_force_longhorn_beetle_gu", () -> new BruteForceLonghornBeetleGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Normal [基础力道]
    public static final DeferredHolder<MobEffect, CrashGuEffect> HORIZONTAL_CRASH_GU = MOB_EFFECTS.register(
            "horizontal_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, CrashGuEffect> VERTICAL_CRASH_GU = MOB_EFFECTS.register(
            "vertical_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, CrashGuEffect> CHARGING_CRASH_GU = MOB_EFFECTS.register(
            "charging_crash_gu", () -> new CrashGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> SELF_RELIANCE_GU = MOB_EFFECTS.register(
            "self_reliance_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, HardshipStrengthGuEffect> HARDSHIP_STRENGTH_GU =
            MOB_EFFECTS.register("hardship_strength_gu", () -> new HardshipStrengthGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> ALL_OUT_EFFORT_GU = MOB_EFFECTS.register(
            "all_out_effort_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MarkerEffect> STORE_STRENGTH_GU = MOB_EFFECTS.register(
            "store_strength_gu", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion
    //endregion

    //region Wisdom Path [智道]
    public static final DeferredHolder<MobEffect, CasualGuEffect> CASUAL_GU = MOB_EFFECTS.register(
            "casual_gu", () -> new CasualGuEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    public static final DeferredHolder<MobEffect, MaliciousThoughtGuEffect> MALICIOUS_THOUGHT_GU = MOB_EFFECTS.register(
            "malicious_thought_gu", () -> new MaliciousThoughtGuEffect(
                    MobEffectCategory.BENEFICIAL, EFFECT_COLOR, new long[] { 2L, 20L, 200L, 2_000L }));
    //endregion

    //region Blood Path [血道]
    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING = MOB_EFFECTS.register(
            "bleeding", () -> new BleedingEffect(MobEffectCategory.HARMFUL, EFFECT_COLOR));
    //endregion

    //region Food Path [食道]
    public static final DeferredHolder<MobEffect, MarkerEffect> LIQUOR_WORM = MOB_EFFECTS.register(
            "liquor_worm", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, EFFECT_COLOR));
    //endregion

    //region Transformation Path [变化道]
    public static final DeferredHolder<MobEffect, MarkerEffect> TEMP_ZOMBIE = MOB_EFFECTS.register(
            "temp_zombie", () -> new MarkerEffect(MobEffectCategory.NEUTRAL, EFFECT_COLOR));
    //endregion

    public static MobEffectInstance instance(@NotNull Holder<MobEffect> effect, int duration) {
        return new MobEffectInstance(effect, duration, 0, false, false, true);
    }

    public static MobEffectInstance instance(@NotNull Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, false, true);
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
