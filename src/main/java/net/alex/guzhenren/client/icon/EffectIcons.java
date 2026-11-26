package net.alex.guzhenren.client.icon;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 状态效果与客户端图标渲染策略的集中对照表。
 * <ul>
 *   <li>将映射集中在一处维护，客户端入口类（{@code GuzhenrenClient}）只需把每对映射注册给 NeoForge；{@link #forEach} 按
 *       固定顺序遍历，为每个效果注册表句柄绑定 {@link EffectIconLayout} 图标或无图标占位 {@link NoEffectIcon}。</li>
 * </ul>
 */

public final class EffectIcons {

    private EffectIcons() {}

    public static void forEach(
            @NotNull BiConsumer<IClientMobEffectExtensions, Supplier<? extends MobEffect>> register) {
        register.accept(GradedEffectIcon.createMobEffect("all_out_effort_gu", 3, 5), ModEffects.ALL_OUT_EFFORT_GU);
        register.accept(GradedEffectIcon.createMobEffect("liquor_worm", 1, 4), ModEffects.LIQUOR_WORM);
        register.accept(GradedEffectIcon.createItem("casual_gu", 1, 2), ModEffects.CASUAL_GU);
        register.accept(GradedEffectIcon.createItem("malicious_thought_gu", 2, 5, 2), ModEffects.MALICIOUS_THOUGHT_GU);
        register.accept(GradedEffectIcon.createItem("self_reliance_gu", 2, 4), ModEffects.SELF_RELIANCE_GU);
        register.accept(new ItemEffectIcon("hardship_strength_gu_4"), ModEffects.HARDSHIP_STRENGTH_GU);
        register.accept(new ItemEffectIcon("store_strength_gu_3"), ModEffects.STORE_STRENGTH_GU);
        register.accept(new ItemEffectIcon("habitual_strength_gu_4"), ModEffects.HABITUAL_STRENGTH_GU);
        register.accept(new ItemEffectIcon("big_strength_gu_2"), ModEffects.BIG_STRENGTH_GU);
        register.accept(new ItemEffectIcon("jade_skin_gu_1"), ModEffects.JADE_SKIN_GU);
        register.accept(new ItemEffectIcon("heavenly_mugwort_gu_3"), ModEffects.HEAVENLY_MUGWORT_GU);
        register.accept(new ItemEffectIcon("white_jade_gu_2"), ModEffects.WHITE_JADE_GU);
        register.accept(new ItemEffectIcon("strength_qi_gu_3"), ModEffects.STRENGTH_QI_GU);
        register.accept(new ItemEffectIcon("horizontal_crash_gu_3"), ModEffects.HORIZONTAL_CRASH_GU);
        register.accept(new ItemEffectIcon("vertical_crash_gu_3"), ModEffects.VERTICAL_CRASH_GU);
        register.accept(GradedEffectIcon.createItem("charging_crash_gu", 4, 5), ModEffects.CHARGING_CRASH_GU);
        register.accept(new ItemEffectIcon("second_watch_gu_4"), ModEffects.SECOND_WATCH_GU);
        register.accept(new ItemEffectIcon("third_watch_gu_5"), ModEffects.THIRD_WATCH_GU);
        register.accept(new NoEffectIcon(), ModEffects.TEMPORARY_ZOMBIE);
    }
}
