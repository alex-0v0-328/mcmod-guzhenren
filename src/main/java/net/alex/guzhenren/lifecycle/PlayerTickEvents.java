package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.SprintSpeedService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 服务端玩家每刻（{@link PlayerTickEvent.Post}）末尾的事件入口，依次驱动 {@link Heartbeat#beat}、{@link AutoUse#tick}
 * （高频自动催动停止判定与自力更生蛊[Self Reliance Gu]治疗脉冲）以及 {@link SprintSpeedService#refresh}（兽类疾跑移速刷
 * 新）；仅限拳头生效的攻击加成随换手事件更新，不经由本刻循环。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerTickEvents {

    private PlayerTickEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Heartbeat.beat(player);
        AutoUse.tick(player);
        SprintSpeedService.refresh(player);
    }
}
