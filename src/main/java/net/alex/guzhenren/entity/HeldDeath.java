package net.alex.guzhenren.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 动画生物共用的死亡维持工具：死亡动画播放期间将实体固定在原地，待达到覆盖整段死亡动画的 {@code removeTick} 时再广播原
 * 版死亡烟雾并移除实体。
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
