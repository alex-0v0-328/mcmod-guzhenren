package net.alex.guzhenren.effect;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * A timed beast buff [兽力] that leaves a ten-second weakness aftermath. Subclasses add the buff itself (attack
 * through {@code AttackContributor}, or an attribute modifier); this class owns the penalty and the thirty-second
 * default length, which a subclass of another length hides with its own {@code DURATION_TICKS}.
 *
 * <p>The aftermath is applied on the effect's last tick ({@code duration == 1}), because a
 * {@link MobEffect} has no expiry hook. Changing how that last tick is recognized is how the penalty
 * quietly stops happening. ⚠ The aftermath is a punishment -- milk or {@code /effect clear} skipping it
 * is the intended design, NOT a gap.
 *
 * <p>⚠ Only a penalty may ride the last tick, which milk, {@code /effect clear} and death never reach. A refund,
 * a payback or a debt (the Liquor Worm's 1:2 return, the Death Qi lifespan bill) reads its own effect's level on
 * the heartbeat instead.
 *
 * <p>⚠ This class answers vanilla's per-tick callbacks, so its subclasses carry no tick code. {@code applyEffectTick}
 * answers {@code true}: a {@code false} has vanilla remove the effect then and there.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class AftermathEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;
    public static final int AFTERMATH_TICKS = 10 * Ticks.SECOND;
    private static final int LAST_TICK = 1;

    protected AftermathEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration == LAST_TICK; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(ModEffects.instance(MobEffects.WEAKNESS, AFTERMATH_TICKS));
        return true;
    }
}
