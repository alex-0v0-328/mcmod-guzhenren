package net.alex.guzhenren.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads {@code LivingEntity#lastHurt}, the hit already taken inside the current invulnerability frames.
 *
 * <p>The incoming-damage event fires before vanilla's invulnerability check, so a damage rule that spends a
 * resource there ({@code MugwortShieldService#absorb}) reads this to spend only on the part of a hit that will
 * land.
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
