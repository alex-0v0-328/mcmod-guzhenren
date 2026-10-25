package net.alex.guzhenren.gameplay.rewind;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Hands every server tick to {@link ChunkRewindService#tickQueue}, which restores one queued chunk per tick so a
 * Mountain or River As Before Gu [山如故 / 江如故] spreads its nine chunks over nine ticks instead of one.
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
}
