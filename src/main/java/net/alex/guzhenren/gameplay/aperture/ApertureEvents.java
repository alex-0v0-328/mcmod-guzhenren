package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 空窍的事件订阅入口。
 * <ul>
 *   <li>十绝体[Ten Extreme Physiques]变更时转交 {@link ApertureService#onExtremePhysiqueChanged} 同步天赋流派效果与第一
 *       空窍基础真元。</li>
 *   <li>死亡与换维度时调 {@link ApertureNourishService#cancel} 停止温养[Nourish]：温养记录随死亡继承，不停则
 *       {@code keepInventory} 重生后仍锁移动、每秒扣真元；宝黄天[Treasure Yellow Heaven]不许温养。死亡在最低优先级处
 *       理，被图腾等取消的死亡收不到。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ApertureEvents {

    private ApertureEvents() {}

    @SubscribeEvent
    public static void onExtremePhysiqueChanged(ExtremePhysiqueChangedEvent event) {
        ApertureService.onExtremePhysiqueChanged(event.getPlayer(), event.getBefore(), event.getAfter());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ApertureNourishService.cancel(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ApertureNourishService.cancel(player);
    }
}
