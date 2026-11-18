package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家击退抗性临时属性修饰符服务：仅承载铁骨（{@code Bone.getKnockbackResistance}）提供的击退抗性。
 * <ul>
 *   <li>由 {@link AttributeEvents} 在 {@code BodyTemperedEvent} 时刷新，并在登录、玩家克隆（临时修饰符不随克隆保留）与
 *       角色重置时重新同步。</li>
 *   <li>⚠ 必须使用 {@link TransientModifiers} 挂载临时修饰符；原生骨的抗性加成为 {@code 0}，
 *       {@link TransientModifiers#swap} 会自动移除修饰符。</li>
 * </ul>
 */

public final class KnockbackResistanceService {

    private KnockbackResistanceService() {}

    private static final ResourceLocation MODIFIER_ID = Guzhenren.id("bone_knockback_resistance");

    public static void refresh(@NotNull ServerPlayer player) {
        TransientModifiers.swap(player, Attributes.KNOCKBACK_RESISTANCE, MODIFIER_ID,
                BodyService.getBone(player).getKnockbackResistance());
    }
}
