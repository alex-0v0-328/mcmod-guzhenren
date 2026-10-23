package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.FirstApertureInsertedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * What aperture storage does when the apertures [空窍] change under it: a first aperture inserted ahead of a lone
 * second one shifts every stored list and the Vital Gu [本命蛊] slot through
 * {@link ApertureStorageService#shiftForFirstAperture}.
 *
 * @author Alex
 * @version 1.0.0
 * @see FirstApertureInsertedEvent
 * @see ApertureStorageService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ApertureStorageEvents {

    private ApertureStorageEvents() {}

    @SubscribeEvent
    public static void onFirstApertureInserted(FirstApertureInsertedEvent event) {
        ApertureStorageService.shiftForFirstAperture(event.getPlayer());
    }
}
