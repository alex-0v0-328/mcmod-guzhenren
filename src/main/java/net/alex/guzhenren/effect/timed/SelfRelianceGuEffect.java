package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Self Reliance Gu [自力更生蛊] effect: a slow self-heal that only mends what sits below its grade's limit.
 *
 * <p>One pulse per second, healing {@code grade + 1} HP up to a limit between half and seventy
 * percent of max health by grade. The item also drives itself at the brink -- see
 * {@link net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem#tryAutoUse}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem
 * @since 1.0.0
 */

public final class SelfRelianceGuEffect extends MobEffect {

    private static final float HEAL_CAP_BASE = 0.5F;
    private static final float HEAL_CAP_TIER_STEP = 0.1F;

    public SelfRelianceGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % Ticks.SECOND == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int tier = Math.clamp(amplifier - 1, 0, 2);
        float limit = entity.getMaxHealth() * (HEAL_CAP_BASE + tier * HEAL_CAP_TIER_STEP);
        if (entity.getHealth() < limit) entity.heal(Math.min(tier + 1.0F, limit - entity.getHealth()));
        return true;
    }
}
