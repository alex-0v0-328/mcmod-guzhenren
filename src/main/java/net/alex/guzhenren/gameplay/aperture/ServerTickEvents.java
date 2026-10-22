package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Drives the world-level background work that outlives the player who caused it: the staged
 * pressure-explosion [空窍压力爆炸] craters. Unlike the one-second heartbeat in {@link
 * net.alex.guzhenren.lifecycle.Heartbeat} these runs every server tick and keeps running
 * after the player died -- the crater finishes on its own. The blasts live in each level's saved
 * data, not in a static list, so a server stop mid-blast resumes on the next start and a
 * singleplayer world switch never carries stale level references.
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureExplosions
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ServerTickEvents {

    private ServerTickEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) AperturePressureExplosions.tickAll(event.getServer());
    }
}
