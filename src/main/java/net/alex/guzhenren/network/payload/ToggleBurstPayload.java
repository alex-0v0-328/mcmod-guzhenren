package net.alex.guzhenren.network.payload;


/**
 * The client-intent payload for a sprint double tap: start a burst [疾飞], or end the one in progress.
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.flight.FlightService#toggleBurst} decides from the server's own state, and a
 * player without the burst grant simply gets no burst.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.FlightService
 * @since 1.0.0
 */

public record ToggleBurstPayload() {

    public static final ToggleBurstPayload INSTANCE = new ToggleBurstPayload();
}
