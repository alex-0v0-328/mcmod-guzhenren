package net.alex.guzhenren.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads {@code LivingEntity#lastHurt}, the hit the current invulnerability frames already took; server side.
 *
 * <p>{@code ShieldService.guard} hands a hit inside the frames on as {@code lastHurt} plus what the shield [护盾]
 * passed, because vanilla subtracts {@code lastHurt} again before the hit lands.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("lastHurt")
    float guzhenren$getLastHurt();
}
