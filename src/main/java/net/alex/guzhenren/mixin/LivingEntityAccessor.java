package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lands {@link LastHurtView} on {@code LivingEntity}: the accessor of its {@code lastHurt} field.
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
