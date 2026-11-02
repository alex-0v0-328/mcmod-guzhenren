package net.alex.guzhenren.gameplay.body;

/**
 * Every living entity's {@code LivingEntity#lastHurt}, the hit its current invulnerability frames already took;
 * server side, put on every {@code LivingEntity} by {@code LivingEntityAccessor}.
 *
 * <p>{@code ShieldService.guard} hands a hit inside the frames on as {@code lastHurt} plus what the shield [护盾]
 * passed, because vanilla subtracts {@code lastHurt} again before the hit lands.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public interface LastHurtView {

    float guzhenren$getLastHurt();
}
