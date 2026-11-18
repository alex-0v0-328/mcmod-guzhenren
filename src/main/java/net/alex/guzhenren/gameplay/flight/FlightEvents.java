package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 飞行与二段飞行[Second-Stage Flight]的事件转发入口：每个服务端玩家 tick 末尾先执行 {@link FlightService#tick} 结算蛊虫
 * 飞行授权，再执行 {@link SecondStageService#tick} 推进二段飞行（确保二段飞行读到本 tick 最新的飞行状态）；玩家登出时调
 * {@link SecondStageService#forget} 清理撞击冷却表。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class FlightEvents {

    private FlightEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FlightService.tick(player);
            SecondStageService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SecondStageService.forget(event.getEntity());
    }
}
