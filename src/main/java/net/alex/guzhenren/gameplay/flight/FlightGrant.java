package net.alex.guzhenren.gameplay.flight;

import org.jetbrains.annotations.NotNull;

/**
 * What a source lets a player do in the air, as a ladder: {@code NONE}, flight [飞行] (creative-style flight at
 * its normal speed) or burst [疾飞] (that flight at {@link FlightService#BURST_MULTIPLIER} times the speed, which
 * includes flight). {@link #getStrongest} keeps the higher rung, so several sources never add up past the top one.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightContributor
 * @since 1.0.0
 */

public enum FlightGrant {

    NONE,
    FLIGHT,
    BURST;

    public boolean allowsFlight() { return this != NONE; }

    public boolean allowsBurst() { return this == BURST; }

    public static @NotNull FlightGrant getStrongest(@NotNull FlightGrant first, @NotNull FlightGrant second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
