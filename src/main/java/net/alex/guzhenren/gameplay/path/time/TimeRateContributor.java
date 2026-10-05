package net.alex.guzhenren.gameplay.path.time;

/**
 * The one seam anything hastens the player's own clock [自身时间] through: {@link PathTimeFlowService#getRate}
 * walks {@code getActiveEffects()} and adds up every contributor's {@link #timeRate}, instead of naming the
 * effects it knows about -- the same shape as {@code AttackContributor} for attack.
 *
 * <p>The effect side implements it; the path side only reads it, so the Time Path [宙道] does not depend on
 * the effect package.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathTimeFlowService
 * @since 1.0.0
 */

public interface TimeRateContributor {

    int timeRate(int amplifier);
}
