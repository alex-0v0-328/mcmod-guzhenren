package net.alex.guzhenren.gameplay.aperture.pressure;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 空窍[Aperture]压力爆炸坑的世界级后台驱动：每服务端 tick（非每秒 {@link net.alex.guzhenren.lifecycle.Heartbeat}）
 * 分段推进，玩家死后照挖。
 * <ul>
 *   <li>爆炸存各维度存档数据：停服续挖，单人切世界无过期维度引用。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureExplosions
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AperturePressureEvents {

    private AperturePressureEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AperturePressureExplosions.tickAll(event.getServer());
    }
}
