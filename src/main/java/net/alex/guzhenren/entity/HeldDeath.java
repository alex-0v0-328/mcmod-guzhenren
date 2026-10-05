package net.alex.guzhenren.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The death the animated mobs share: the body holds still where it fell while its death animation plays, and
 * goes with vanilla's death poof at a tick that covers the whole animation.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class HeldDeath {

    private HeldDeath() {}

    static void tick(LivingEntity entity, int removeTick) {
        entity.deathTime++;
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity.deathTime >= removeTick && !entity.level().isClientSide() && !entity.isRemoved()) {
            entity.level().broadcastEntityEvent(entity, EntityEvent.POOF);
            entity.remove(Entity.RemovalReason.KILLED);
        }
    }
}
