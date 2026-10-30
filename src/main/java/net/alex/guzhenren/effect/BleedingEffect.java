package net.alex.guzhenren.effect;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Bleeding [流血]: 1 damage a second, past armor, for as long as it lasts. The Blood Moon Blade [血月刃] leaves it.
 *
 * <p>A plain effect with no state behind it, so milk cures it, the one exception in {@link MilkCureEvents}. The
 * pulse names no attacker; the kill goes to whoever last hurt the target, as with poison.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.projectile.MoonBladeEntity
 * @since 1.0.0
 */

public final class BleedingEffect extends PeriodicEffect {

    public static final float DAMAGE_PER_PULSE = 1.0F;

    public BleedingEffect(MobEffectCategory category, int color) {
        super(category, color, Ticks.SECOND);
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.BLEEDING), DAMAGE_PER_PULSE);
    }
}
