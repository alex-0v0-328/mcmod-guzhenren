package net.alex.guzhenren.advancement;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.RealmReachedEvent;
import net.alex.guzhenren.gameplay.aperture.awakening.ApertureAwakenedEvent;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 玩法事件转进度触发器：{@link RealmReachedEvent} 触发 {@link RankReachedTrigger}，{@link ApertureAwakenedEvent} 触发
 * {@link HopeGuUsedTrigger}；玩法层在进度之下，只发事件不上调。
 *
 * @author Alex
 * @version 1.0.0
 * @see ModCriteriaTriggers
 * @since 1.0.0
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
