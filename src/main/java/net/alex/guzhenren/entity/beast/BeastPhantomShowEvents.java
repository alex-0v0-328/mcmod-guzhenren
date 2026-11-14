package net.alex.guzhenren.entity.beast;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.BeastPhantomsDrawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 为攻击轮次立兽力虚影的订阅类：事件同步分发，故同 tick 以 {@link BeastPhantomEntity#show} 响应
 * {@link BeastPhantomsDrawnEvent}；轮次层级在实体之下，只发事件，不向上调用。
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomShowEvents {

    private BeastPhantomShowEvents() {}

    @SubscribeEvent
    public static void onPhantomsDrawn(BeastPhantomsDrawnEvent event) {
        BeastPhantomEntity.show(event.getPlayer(), event.getFamilies());
    }
}
