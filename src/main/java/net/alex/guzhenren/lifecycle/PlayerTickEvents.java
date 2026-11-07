package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.SprintSpeedService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands every server player's end-of-tick to {@link Heartbeat}, then to the auto Gu states' stop check and the Self
 * Reliance heal pulse ({@link AutoUse#tick}), which run faster than the beat, then to the attack refresh
 * ({@link AttackDamageService#refresh}), so a hand change moves the fist-only strength bonus the same tick, and the
 * beasts' speed ({@link SprintSpeedService#refresh}), so it follows the sprint's toggle and the round's release; the
 * event handler lives here, the heartbeat's order there.
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
        AutoUse.tick(player);
        AttackDamageService.refresh(player);
        SprintSpeedService.refresh(player);
    }
}
