package net.alex.guzhenren.advancement;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.RealmReachedEvent;
import net.alex.guzhenren.gameplay.aperture.awakening.ApertureAwakenedEvent;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 将玩法层事件桥接至原版进度触发器的事件监听类：监听 {@link RealmReachedEvent} 触发修为进阶条件
 * {@link RankReachedTrigger}，监听 {@link ApertureAwakenedEvent} 触发希望蛊开窍条件 {@link HopeGuUsedTrigger}，使底层玩
 * 法包无需反向依赖进度包。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AdvancementEvents {

    private AdvancementEvents() {}

    @SubscribeEvent
    public static void onRealmReached(RealmReachedEvent event) {
        ModCriteriaTriggers.REACHED_RANK.get().trigger(event.getPlayer(), event.getAperture());
    }

    @SubscribeEvent
    public static void onApertureAwakened(ApertureAwakenedEvent event) {
        ModCriteriaTriggers.USED_HOPE_GU.get().trigger(event.getPlayer());
    }
}
