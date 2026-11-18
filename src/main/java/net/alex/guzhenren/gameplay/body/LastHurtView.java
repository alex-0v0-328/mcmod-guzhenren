package net.alex.guzhenren.gameplay.body;

/**
 * 读取 {@code LivingEntity.lastHurt}（当前无敌帧内已结算的最大伤害值）的桥接接口，由 {@code LivingEntityAccessor} 注
 * 入；因原版在无敌帧内放行差额伤害前会先减去 {@code lastHurt}，{@link ShieldService#guard} 将穿透护盾的余量加回该值以还
 * 原真实穿透伤害。
 */

public interface LastHurtView {

    float guzhenren$getLastHurt();
}
