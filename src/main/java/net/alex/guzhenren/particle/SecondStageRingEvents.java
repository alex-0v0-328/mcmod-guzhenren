package net.alex.guzhenren.particle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.gameplay.flight.SecondStageStartedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * {@link SecondStageRingEmitter} 的服务端事件桥接器：在玩家刻末尾推进音障环轨迹，在登出或切换维度时清理轨迹，并分别响应
 * {@link SecondStageStartedEvent} 与服务端 {@link SecondStageCrashEvent} 发送音爆环与撞击环。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class SecondStageRingEvents {

    private SecondStageRingEvents() {}

    @SubscribeEvent
    public static void onSecondStageStarted(SecondStageStartedEvent event) {
        SecondStageRingEmitter.sonicBoom(event.getPlayer());
    }

    @SubscribeEvent
    public static void onSecondStageCrash(SecondStageCrashEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) SecondStageRingEmitter.crash(player, event.getHeading());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) SecondStageRingEmitter.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SecondStageRingEmitter.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SecondStageRingEmitter.forget(event.getEntity());
    }
}
