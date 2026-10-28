package net.alex.guzhenren.client.icon;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * Which effect wears which icon: the one table both game versions read, so the client entry point only hands each
 * pair to its loader ({@code GuzhenrenClient}).
 *
 * <p>{@link #forEach} walks the table in a fixed order, one icon per effect handle: an {@link EffectIconLayout}, or a
 * {@link NoEffectIcon} for an effect shown without one.
 *
 * @author Alex
 * @version 1.0.0
 * @see GradedEffectIcon
 * @see ItemEffectIcon
 * @since 1.0.0
 */

public final class EffectIcons {

    private EffectIcons() {}

    public static void forEach(
            @NotNull BiConsumer<IClientMobEffectExtensions, Supplier<? extends MobEffect>> register) {
        register.accept(GradedEffectIcon.createMobEffect("essence_qi", 1, 5), ModEffects.ESSENCE_QI);
        register.accept(GradedEffectIcon.createMobEffect("life_qi", 1, 5), ModEffects.LIFE_QI);
        register.accept(GradedEffectIcon.createMobEffect("strength_qi", 1, 5), ModEffects.STRENGTH_QI);
        register.accept(GradedEffectIcon.createMobEffect("all_out_effort", 3, 5), ModEffects.ALL_OUT_EFFORT);
        register.accept(GradedEffectIcon.createMobEffect("liquor_worm", 1, 4), ModEffects.LIQUOR_WORM);
        register.accept(GradedEffectIcon.createItem("casual_gu", 1, 2), ModEffects.CASUAL_GU);
        register.accept(GradedEffectIcon.createItem("malicious_thought_gu", 2, 5, 2), ModEffects.MALICIOUS_THOUGHT_GU);
        register.accept(GradedEffectIcon.createItem("self_reliance_gu", 2, 4), ModEffects.SELF_RELIANCE_GU);
        register.accept(new ItemEffectIcon("hardship_strength_gu"), ModEffects.HARDSHIP_STRENGTH_GU);
        register.accept(new ItemEffectIcon("jade_skin_gu"), ModEffects.JADE_SKIN_GU);
        register.accept(new ItemEffectIcon("heavenly_mugwort_gu"), ModEffects.HEAVENLY_MUGWORT_GU);
        register.accept(new ItemEffectIcon("white_jade_gu"), ModEffects.WHITE_JADE_GU);
        register.accept(new ItemEffectIcon("horizontal_crash_gu"), ModEffects.HORIZONTAL_CRASH_GU);
        register.accept(new ItemEffectIcon("vertical_crash_gu"), ModEffects.VERTICAL_CRASH_GU);
        register.accept(GradedEffectIcon.createItem("charging_crash_gu", 4, 5), ModEffects.CHARGING_CRASH_GU);
        register.accept(new ItemEffectIcon("second_watch_gu"), ModEffects.SECOND_WATCH_GU);
        register.accept(new ItemEffectIcon("third_watch_gu"), ModEffects.THIRD_WATCH_GU);
        register.accept(new NoEffectIcon(), ModEffects.HALF_ZOMBIE);
    }
}
