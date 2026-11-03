package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Hardship Strength Gu [苦力蛊] effect: for as long as it runs the body brings every strength point [力量点] to bear,
 * the carrying capacity [承受力] set aside and no load [负载] taken, and in exchange healing stops at
 * {@code HardshipEvents.HEAL_CEILING} of the max health.
 *
 * <p>This class carries only the duration and the icon; the lift is read by {@code StrengthService.isUnleashed}
 * and the ceiling enforced by {@code HardshipEvents} on every heal, both off this effect's presence. A body above
 * the ceiling when the effect starts loses nothing: it only stops healing until it drops below. When the effect
 * ends the ceiling goes with it and nothing is refunded. The Grievous Injury [重伤] marker is this effect's harmful
 * icon, projected beside it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.body.HardshipEvents
 * @since 1.0.0
 */

public final class HardshipStrengthGuEffect extends MobEffect {

    public static final int DURATION_TICKS = 120 * Ticks.SECOND;

    public HardshipStrengthGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
