package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * Knockback resistance as a transient {@link net.minecraft.world.entity.ai.attributes.AttributeModifier}, the
 * bones' [骨] share alone ({@code Bone.getKnockbackResistance}). Static service; {@link AttributeEvents}
 * refreshes it on every {@code BodyTemperedEvent}, plus login, clone and reset (a modifier does not ride a
 * clone). Native bones read 0, which {@link TransientModifiers#swap} writes as no modifier.
 *
 * <p>⚠ The modifier MUST stay transient ({@link TransientModifiers}).
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
        AttributeInstance instance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (instance == null) return;

        TransientModifiers.swap(instance, MODIFIER_ID, BodyService.getBone(player).getKnockbackResistance());
    }
}
