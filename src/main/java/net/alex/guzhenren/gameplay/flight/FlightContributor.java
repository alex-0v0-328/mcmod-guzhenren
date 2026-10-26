package net.alex.guzhenren.gameplay.flight;

/**
 * The one seam a flying Gu effect grants flight through: an effect implements it and returns, for its amplifier,
 * flight alone or second-stage flight [二段飞行] as well. {@link FlightService} walks {@code getActiveEffects()}
 * every tick and keeps the strongest grant, instead of listing the effects it knows about -- a new flying Gu is one
 * interface, no edit there. The grant lasts exactly as long as the effect, and nothing about it reaches the save.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

public interface FlightContributor {

    FlightGrant getFlightGrant(int amplifier);
}
