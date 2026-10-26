package net.alex.guzhenren.network.payload;


/**
 * The client-intent payload for a sprint double tap: enter second-stage flight [二段飞行], or leave it.
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.flight.SecondStageService#toggle} decides from the server's own state, and a
 * player who may not enter simply stays out.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.SecondStageService
 * @since 1.0.0
 */

public record ToggleSecondStagePayload() {

    public static final ToggleSecondStagePayload INSTANCE = new ToggleSecondStagePayload();
}
