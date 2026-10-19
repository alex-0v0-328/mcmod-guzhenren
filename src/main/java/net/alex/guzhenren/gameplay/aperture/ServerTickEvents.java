package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives the world-level background work that outlives the player who caused it: the staged
 * pressure-explosion [空窍压力爆炸] craters. Unlike the one-second heartbeat in {@link
 * net.alex.guzhenren.lifecycle.PlayerTickEvents} these runs every server tick and keeps running
 * after the player died -- the crater finishes on its own. The blasts live in each level's saved
 * data, not in a static list, so a server stop mid-blast resumes on the next start and a
 * singleplayer world switch never carries stale level references.
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureExplosions
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ServerTickEvents {

    private ServerTickEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AperturePressureExplosions.tickAll(event.getServer());
    }
}
