package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hands every server player's end-of-tick to {@link Heartbeat}. Loader glue only: the tick event differs
 * between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port, the heartbeat behind it does not.
 *
 * @author Alex
 * @version 1.0.0
 * @see Heartbeat
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerTickEvents {

    private PlayerTickEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) Heartbeat.beat(player);
    }
}
