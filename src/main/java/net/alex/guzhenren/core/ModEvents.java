package net.alex.guzhenren.core;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

/**
 * Posts this mod's own events on the game event bus, where a lower layer tells a higher one that something
 * changed (an aperture, a physique, a strength path, a lost Vital Gu).
 *
 * <p>⚠ A version seam: services post through {@link #post} only, so the posting line stays identical between
 * this NeoForge 1.21.1 mainline and the Forge 1.20.1 port; the event classes extend each loader's own
 * {@code Event}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEvents {

    private ModEvents() {}

    public static void post(@NotNull Event event) { NeoForge.EVENT_BUS.post(event); }
}
