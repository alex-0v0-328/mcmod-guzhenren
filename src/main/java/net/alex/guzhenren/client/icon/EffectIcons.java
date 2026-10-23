package net.alex.guzhenren.client.icon;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

/**
 * Which effect wears which icon: the one table both game versions read, so the client entry point only hands each
 * pair to its loader ({@code GuzhenrenClient}).
 *
 * <p>{@link #forEach} walks the table in a fixed order, one {@link EffectIconLayout} per effect handle.
 *
 * @author Alex
 * @version 1.0.0
 * @see GradedEffectIcon
 * @see ItemEffectIcon
 * @since 1.0.0
 */

public final class EffectIcons {

    private EffectIcons() {}

    public static void forEach(@NotNull BiConsumer<EffectIconLayout, Supplier<? extends MobEffect>> register) {
        register.accept(GradedEffectIcon.mobEffect("essence_qi", 1, 5), ModEffects.ESSENCE_QI);
        register.accept(GradedEffectIcon.mobEffect("life_qi", 1, 5), ModEffects.LIFE_QI);
        register.accept(GradedEffectIcon.mobEffect("strength_qi", 1, 5), ModEffects.STRENGTH_QI);
        register.accept(GradedEffectIcon.mobEffect("all_out_effort", 3, 5), ModEffects.ALL_OUT_EFFORT);
        register.accept(GradedEffectIcon.mobEffect("liquor_worm", 1, 4), ModEffects.LIQUOR_WORM);
        register.accept(GradedEffectIcon.item("casual_gu", 1, 2), ModEffects.CASUAL_GU);
        register.accept(GradedEffectIcon.item("malicious_thought_gu", 2, 5, 2), ModEffects.MALICIOUS_THOUGHT_GU);
        register.accept(GradedEffectIcon.item("self_reliance_gu", 2, 4), ModEffects.SELF_RELIANCE_GU);
        register.accept(new ItemEffectIcon("hardship_strength_gu"), ModEffects.HARDSHIP_STRENGTH_GU);
        register.accept(new ItemEffectIcon("horizontal_crash_gu"), ModEffects.HORIZONTAL_CRASH_GU);
        register.accept(new ItemEffectIcon("vertical_crash_gu"), ModEffects.VERTICAL_CRASH_GU);
        register.accept(GradedEffectIcon.item("charging_crash_gu", 4, 5), ModEffects.CHARGING_CRASH_GU);
        register.accept(new ItemEffectIcon("second_watch_gu"), ModEffects.SECOND_WATCH_GU);
        register.accept(new ItemEffectIcon("third_watch_gu"), ModEffects.THIRD_WATCH_GU);
    }
}
