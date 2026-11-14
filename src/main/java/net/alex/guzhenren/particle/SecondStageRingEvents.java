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
 * {@link SecondStageRingEmitter} 的事件入口：玩家 tick 末推进音障环，登出或换维度时丢弃其轨迹。
 * 每个 {@link SecondStageStartedEvent} 发音爆环，每个服务端 {@link SecondStageCrashEvent} 发撞击环。
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageRingEmitter
 * @since 1.0.0
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
