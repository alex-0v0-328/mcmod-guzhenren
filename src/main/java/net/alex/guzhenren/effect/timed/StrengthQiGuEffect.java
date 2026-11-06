package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Strength Qi Gu [力气蛊] effect: for as long as it runs an empty-hand punch at Epic Fight's lock-on target past the
 * vanilla reach throws a Qi Wave [气波] or, on the beast strength phantom path, a Beast Charge [兽力冲击] at it
 * instead, at any distance and once a strength round at most ({@code QiWaveService}).
 *
 * <p>This class carries only the duration and the icon; the buff's truth is its timed path effect
 * ({@code TimedEffects.hasBuff}), which the server reads, while the client reads this effect to decide whether a
 * punch is worth sending.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.projectile.QiWaveEntity
 * @see net.alex.guzhenren.entity.projectile.BeastChargeEntity
 * @since 1.0.0
 */

public final class StrengthQiGuEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;

    public StrengthQiGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
