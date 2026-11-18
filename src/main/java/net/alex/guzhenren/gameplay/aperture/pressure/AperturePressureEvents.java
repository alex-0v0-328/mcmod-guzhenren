package net.alex.guzhenren.gameplay.aperture.pressure;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 空窍压力爆炸坑的世界级 tick 驱动入口：每个服务端 tick 末尾（而非每秒一次的
 * {@link net.alex.guzhenren.lifecycle.Heartbeat}）调用 {@link AperturePressureExplosions#tickAll} 分段推进地形挖掘，玩
 * 家死后或下线后仍继续挖完。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AperturePressureEvents {

    private AperturePressureEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AperturePressureExplosions.tickAll(event.getServer());
    }
}
