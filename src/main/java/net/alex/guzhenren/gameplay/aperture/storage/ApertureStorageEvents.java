package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.FirstApertureInsertedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 空窍[Aperture]存储事件入口：第一空窍插到单独第二空窍前时，经 {@link ApertureStorageService#shiftForFirstAperture}
 * 上移各列表与本命蛊槽一位。
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
