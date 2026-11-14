package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 肉身变化时的空窍[Aperture]订阅者：新十绝体经 {@link ApertureService#onExtremePhysiqueChanged} 交接天赋道[Path]与基础
 * 真元[Essence]。
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
