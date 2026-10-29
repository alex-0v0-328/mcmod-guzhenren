package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * The timed healing of the Vitality Leaf Gu [生机叶蛊]: heals one HP every half-second for 64 pulses.
 *
 * <p>The truth is the leaf path effect [流派效果] ({@code TimedEffects.grantLeaf}); this vanilla effect is its
 * projection, which pulses while it shows and is put back within a second of {@code /effect clear}. Re-using the
 * Gu while this effect runs is a refusal ({@code vitality_active}).
 *
 * <p>A timed buff alters nothing permanently.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class VitalityLeafEffect extends PeriodicEffect {

    public static final int HEAL_INTERVAL_TICKS = Ticks.HALF_SECOND;
    public static final int DURATION_TICKS = 64 * HEAL_INTERVAL_TICKS;

    public VitalityLeafEffect(MobEffectCategory category, int color) {
        super(category, color, HEAL_INTERVAL_TICKS);
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        entity.heal(1.0F);
    }
}
