package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands every server player's end-of-tick to {@link Heartbeat}. Loader glue only: the tick event differs
 * between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port, the heartbeat behind it does not.
 *
 * @author Alex
 * @version 1.0.0
 * @see Heartbeat
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerTickEvents {

    private PlayerTickEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) Heartbeat.beat(player);
    }
}
