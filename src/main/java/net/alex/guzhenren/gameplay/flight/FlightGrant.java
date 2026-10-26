package net.alex.guzhenren.gameplay.flight;

import org.jetbrains.annotations.NotNull;

/**
 * What a source lets a player do in the air, as a ladder: {@code NONE}, flight [飞行] (creative-style flight, which
 * vanilla sprinting speeds up) or second-stage flight [二段飞行] (flight plus the faster stage
 * {@link SecondStageService} runs). {@link #getStrongest} keeps the higher rung, so several sources never add up past
 * the top one.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightContributor
 * @since 1.0.0
 */

public enum FlightGrant {

    NONE,
    FLIGHT,
    SECOND_STAGE;

    public boolean canFly() { return this != NONE; }

    public boolean canFlySecondStage() { return this == SECOND_STAGE; }

    public static @NotNull FlightGrant getStrongest(@NotNull FlightGrant first, @NotNull FlightGrant second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
