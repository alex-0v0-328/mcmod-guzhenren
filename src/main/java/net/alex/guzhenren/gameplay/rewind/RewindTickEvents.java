package net.alex.guzhenren.gameplay.rewind;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 区块回溯还原队列的服务端 tick 事件订阅入口（核心逻辑位于 {@link ChunkRewindService}）。
 * <ul>
 *   <li>每服务端 tick 末（{@link ServerTickEvent.Post}）调用 {@link ChunkRewindService#tickQueue} 按区段预算还原队首
 *       一批，将山如故[Mountain As Before Gu]或江如故[River As Before Gu]一次催动分摊到至多 160 tick（8 秒）内渐进写回
 *       （{@link ChunkRestoreQueue}）。</li>
 *   <li>⚠ 停服（{@link ServerStoppedEvent}）时必须调用 {@link ChunkRewindService#clear} 清空待还原队列与后台生成
 *       计数，避免静态集合持留已停止世界的维度与区块引用。</li>
 * </ul>
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
