package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ShieldRaisingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The beast strength traits [兽力特性] that reach a body package below the strength path through its events. Today one:
 * every shield [护盾] is raised with {@link PathStrengthData#getShieldMultiplier} times its points (the stone turtle's
 * [石龟负力] 1.5, so the heavenly mugwort's 180 becomes 270), its length untouched. The body package cannot read the
 * strength record itself: it sits below this one, and a call up would close a package cycle.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength#getShieldBonus
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastTraitEvents {

    private BeastTraitEvents() {}

    @SubscribeEvent
    public static void onShieldRaising(ShieldRaisingEvent event) {
        event.setPoints(event.getPoints() * PathStrengthService.get(event.getPlayer()).getShieldMultiplier());
    }
}
