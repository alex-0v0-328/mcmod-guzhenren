package net.alex.guzhenren.network.payload;


/**
 * The client-intent payload for sneak plus Alt in the air: start a plunge [坠击].
 *
 * <p>Carries nothing, so there is nothing to forge;
 * {@link net.alex.guzhenren.gameplay.plunge.PlungeService#start} decides from the server's own state, and a player
 * who may not plunge simply keeps falling or flying.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.plunge.PlungeService
 * @since 1.0.0
 */

public record PlungePayload() {

    public static final PlungePayload INSTANCE = new PlungePayload();
}
