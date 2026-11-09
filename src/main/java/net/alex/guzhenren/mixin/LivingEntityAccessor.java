package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lands {@link LastHurtView} on {@code LivingEntity}: the accessor of its {@code lastHurt} field.
 *
 * <p>⚠ Not an event or an API: {@code lastHurt} is a protected field that no NeoForge event exposes ({@code
 * DamageContainer} carries the reductions, not the last hit). An access transformer would reach it too, but needs the
 * commented {@code accessTransformers} line in {@code build.gradle}; the accessor keeps {@link LastHurtView} the only
 * seam.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor extends LastHurtView {

    @Override
    @Accessor("lastHurt")
    float guzhenren$getLastHurt();
}
