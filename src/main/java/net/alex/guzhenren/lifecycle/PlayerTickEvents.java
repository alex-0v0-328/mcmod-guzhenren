package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.SprintSpeedService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 服务端玩家 tick 末尾的事件入口。
 * <ul>
 *   <li>依次交 {@link Heartbeat}、{@link AutoUse#tick}（自动蛊状态停止检查、自力更生蛊[Self Reliance Gu]治疗脉冲，
 *       快于心跳）、{@link SprintSpeedService#refresh}（兽类速度，跟疾跑开关与一轮释放）。</li>
 *   <li>只限拳头的攻击加成经 {@code AttackDamageEvents} 随换手更新，不走此 tick。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Heartbeat
 * @since 1.0.0
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
