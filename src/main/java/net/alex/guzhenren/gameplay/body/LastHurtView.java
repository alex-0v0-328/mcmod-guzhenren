package net.alex.guzhenren.gameplay.body;

/**
 * 服务端每个 {@code LivingEntity} 的 {@code lastHurt}（当前无敌帧已承受的伤害），由 {@code LivingEntityAccessor} 注入；
 * 原版落地前会再减它，故 {@code ShieldService.guard} 交出帧内伤害时以它加护盾放过量。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public interface LastHurtView {

    float guzhenren$getLastHurt();
}
