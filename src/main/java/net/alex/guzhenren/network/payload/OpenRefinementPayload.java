package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.refinement.RefinementMenu;

/**
 * Client intent: open the refinement [炼蛊] menu.
 *
 * <p>A zero-byte singleton payload -- it carries no data at all, only the button press. The server
 * handler in {@link net.alex.guzhenren.network.ModPayloads} opens the
 * {@link RefinementMenu}. Client intent is the one direction attachment
 * sync cannot carry; no player data travels upstream.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record OpenRefinementPayload() {

    public static final OpenRefinementPayload INSTANCE = new OpenRefinementPayload();
}
