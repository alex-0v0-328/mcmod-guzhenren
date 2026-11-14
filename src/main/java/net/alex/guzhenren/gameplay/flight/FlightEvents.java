package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 飞行的事件处理器，规则在两个服务。每个服务端玩家 tick 末先跑 {@link FlightService#tick} 再跑
 * {@link SecondStageService#tick}，蛊飞行先结算，二段飞行[Second-Stage Flight]才看到本 tick 的飞行；登出清撞人冷却
 * （{@link SecondStageService#forget}）。
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
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
