package net.alex.guzhenren.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 动画生物共用死亡：播放时尸体原地停留，到覆盖整段动画的 tick 随原版死亡烟雾移除。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class HeldDeath {

    private HeldDeath() {}

    public static void tick(LivingEntity entity, int removeTick) {
        entity.deathTime++;
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity.deathTime >= removeTick && !entity.level().isClientSide() && !entity.isRemoved()) {
            entity.level().broadcastEntityEvent(entity, EntityEvent.POOF);
            entity.remove(Entity.RemovalReason.KILLED);
        }
    }
}
