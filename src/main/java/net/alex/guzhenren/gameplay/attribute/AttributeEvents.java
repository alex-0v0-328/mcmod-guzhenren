package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Every moment the max health and the knockback resistance are recomputed outside the heartbeat:
 * {@link ApertureChangedEvent} (the rank sets the max health) and {@link BodyTemperedEvent} (bones move both).
 * The attack modifier has its own subscriber, {@code gameplay.path.strength.AttackDamageEvents}, because it
 * is derived from the strength path.
 *
 * @author Alex
 * @version 1.0.0
 * @see MaxHealthService
 * @see KnockbackResistanceService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AttributeEvents {

    private AttributeEvents() {}

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { MaxHealthService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onBodyTempered(BodyTemperedEvent event) {
        MaxHealthService.refresh(event.getPlayer());
        KnockbackResistanceService.refresh(event.getPlayer());
    }
}
