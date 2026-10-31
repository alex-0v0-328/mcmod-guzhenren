package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceModule;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands every server player's end-of-tick to {@link Heartbeat}, then to the Self Reliance heal pulse
 * ({@link SelfRelianceModule#pulse}), which runs faster than the beat; the event handler lives here, the heartbeat's
 * order there.
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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Heartbeat.beat(player);
        SelfRelianceModule.pulse(player);
    }
}
