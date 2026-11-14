package net.alex.guzhenren.particle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.CrashStruckEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * {@link RingConeEmitter} 的事件入口：每个玩家 tick 末推进该玩家的环轨迹，登出或换维度时丢弃；
 * 每个 {@link CrashStruckEvent} 发一枚范围冲击环。
 *
 * @author Alex
 * @version 1.0.0
 * @see RingConeEmitter
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class RingConeEvents {

    private RingConeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) RingConeEmitter.tick(player);
    }

    @SubscribeEvent
    public static void onCrashStruck(CrashStruckEvent event) {
        RingConeEmitter.areaImpactRing(event.getPlayer().serverLevel(), event.getOrigin(), event.getDirection(),
                event.getRange());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RingConeEmitter.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        RingConeEmitter.forget(event.getEntity());
    }
}
