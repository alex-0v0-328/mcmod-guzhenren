package net.alex.guzhenren.gameplay.clock;

/**
 * The one seam anything hastens the player's own clock [自身时间] through: {@link TimeFlow#getSelfRate} walks
 * {@code getActiveEffects()} and adds up every contributor's {@link #timeRate}, instead of naming the effects it knows
 * about -- the same shape as {@code AttackContributor} for attack.
 *
 * <p>The effect side implements it; the clock side only reads it, so the clock does not depend on the effect package.
 * A place where time itself runs differently is a {@link TimeFlowArea}, not a contributor.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @since 1.0.0
 */

public interface TimeRateContributor {

    int timeRate(int amplifier);
}
