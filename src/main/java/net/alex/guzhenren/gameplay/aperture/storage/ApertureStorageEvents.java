package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.FirstApertureInsertedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 空窍存储事件订阅入口：当希望蛊[Hope Gu]在孤立第二空窍前插入第一空窍（{@link FirstApertureInsertedEvent}）时，调
 * {@link ApertureStorageService#shiftForFirstAperture} 将存蛊列表与本命蛊槽后移一位。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ApertureStorageEvents {

    private ApertureStorageEvents() {}

    @SubscribeEvent
    public static void onFirstApertureInserted(FirstApertureInsertedEvent event) {
        ApertureStorageService.shiftForFirstAperture(event.getPlayer());
    }
}
