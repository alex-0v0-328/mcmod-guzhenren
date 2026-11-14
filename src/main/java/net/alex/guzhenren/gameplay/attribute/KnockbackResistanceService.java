package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * 临时修饰的击退抗性，只有骨的份额（{@code Bone.getKnockbackResistance}）。
 * <ul>
 *   <li>{@link AttributeEvents} 于 {@code BodyTemperedEvent} 及登录、克隆、重置时刷新（修饰不随克隆保留）。</li>
 *   <li>⚠ 修饰须临时（{@link TransientModifiers}）；原生骨为 0，{@link TransientModifiers#swap} 写为无修饰。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MaxHealthService
 * @see BodyService
 * @since 1.0.0
 */

public final class KnockbackResistanceService {

    private KnockbackResistanceService() {}

    private static final ResourceLocation MODIFIER_ID = Guzhenren.id("bone_knockback_resistance");

    public static void refresh(@NotNull ServerPlayer player) {
        TransientModifiers.swap(player, Attributes.KNOCKBACK_RESISTANCE, MODIFIER_ID,
                BodyService.getBone(player).getKnockbackResistance());
    }
}
