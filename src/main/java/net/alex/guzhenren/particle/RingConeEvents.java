package net.alex.guzhenren.particle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.CrashStruckEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * {@link RingConeEmitter} 的服务端事件桥接器：在玩家刻末尾推进冲刺环爆发轨迹，在玩家登出或切换维度时清理状态，并在收到
 * {@link CrashStruckEvent} 时发射范围伤害环。
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
        RingConeEmitter.areaDamageRing(event.getPlayer().serverLevel(), event.getOrigin(), event.getDirection(),
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
