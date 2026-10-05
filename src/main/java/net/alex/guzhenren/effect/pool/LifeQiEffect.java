package net.alex.guzhenren.effect.pool;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Life Qi [生气] effect — a pool projection of the 生气 held in {@link
 * PathQiData}, which heals the holder periodically.
 *
 * <p>Pool effects are rebuilt every heartbeat by {@code PathQiService.syncEffects}, so milk cannot cure
 * them — the pool is the truth. The heal runs on vanilla's own {@code applyEffectTick} cadence
 * ({@code HEAL_INTERVAL_TICKS} = {@link Ticks#HALF_SECOND}), healing {@code amplifier + 1} HP each pulse.
 *
 * <p>⚠ 生气 does NOT refuse when 死气 is present: it pays 死气 down 1:1 first, and only the excess
 * reaches the 生气 pool.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathQiService
 * @since 1.0.0
 */

public class LifeQiEffect extends PeriodicEffect {

    public static final int HEAL_INTERVAL_TICKS = Ticks.HALF_SECOND;

    public LifeQiEffect(MobEffectCategory category, int color) {
        super(category, color, HEAL_INTERVAL_TICKS);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.heal(amplifier + 1.0F);
        return true;
    }
}
