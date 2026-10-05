package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * What the aperture [空窍] does when the body [肉身] changes under it: a new ten-extreme physique [十绝体]
 * hands its talent paths and base essence over through {@link ApertureService#onExtremePhysiqueChanged}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ExtremePhysiqueChangedEvent
 * @see ApertureService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ApertureEvents {

    private ApertureEvents() {}

    @SubscribeEvent
    public static void onExtremePhysiqueChanged(ExtremePhysiqueChangedEvent event) {
        ApertureService.onExtremePhysiqueChanged(event.getPlayer(), event.getBefore(), event.getAfter());
    }
}
