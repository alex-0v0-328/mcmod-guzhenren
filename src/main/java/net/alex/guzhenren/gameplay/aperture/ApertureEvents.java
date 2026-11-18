package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 空窍对肉身变更的事件订阅入口：十绝体[Ten Extreme Physiques]变更时转交
 * {@link ApertureService#onExtremePhysiqueChanged} 同步天赋流派效果与第一空窍基础真元。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ApertureEvents {

    private ApertureEvents() {}

    @SubscribeEvent
    public static void onExtremePhysiqueChanged(ExtremePhysiqueChangedEvent event) {
        ApertureService.onExtremePhysiqueChanged(event.getPlayer(), event.getBefore(), event.getAfter());
    }
}
