package net.alex.guzhenren.gameplay.rewind;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 区块还原队列的服务端 tick 订阅类。每 tick 还原一个排队区块，山如故[Mountain As Before Gu]或江如故[River As Before Gu]
 * 的 25 区块分摊到 25 tick；停服时清空队列与生成计数，免得静态字段钉住已停世界的维度与区块。
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkRewindService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class RewindTickEvents {

    private RewindTickEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ChunkRewindService.tickQueue(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { ChunkRewindService.clear(); }
}
