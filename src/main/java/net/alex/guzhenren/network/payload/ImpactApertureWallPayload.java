package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;

/**
 * Client intent: flush the aperture wall [冲刷窍壁] and take whatever the roll gives.
 *
 * <p>A zero-byte singleton payload -- it carries no data at all, only the button press. The server
 * handler in {@link net.alex.guzhenren.network.ModPayloads} delegates to
 * {@link ApertureNourishService}. Client intent is the one
 * direction attachment sync cannot carry; no player data travels upstream.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record ImpactApertureWallPayload() {

    public static final ImpactApertureWallPayload INSTANCE = new ImpactApertureWallPayload();
}
