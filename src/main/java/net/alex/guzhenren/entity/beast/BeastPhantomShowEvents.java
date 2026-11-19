package net.alex.guzhenren.entity.beast;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.BeastPhantomsDrawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 监听连招轮收尾抽取事件并生成兽力虚影的订阅类：{@link BeastPhantomsDrawnEvent} 同步分发，本类在同一 tick 调用
 * {@link BeastPhantomEntity#show} 更新虚影，使玩法层无需反向依赖实体层。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomShowEvents {

    private BeastPhantomShowEvents() {}

    @SubscribeEvent
    public static void onPhantomsDrawn(BeastPhantomsDrawnEvent event) {
        BeastPhantomEntity.show(event.getPlayer(), event.getFamilies());
    }
}
