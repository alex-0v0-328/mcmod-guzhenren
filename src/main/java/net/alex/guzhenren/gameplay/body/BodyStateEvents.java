package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

/**
 * Reacts to the vanilla moments that change body [肉身] behavior: {@code onBreathe} lets an undead life
 * form breathe underwater. The attack refreshes on effect changes and attacks live in
 * {@code gameplay.attribute.AttributeEvents}.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BodyStateEvents {

    private BodyStateEvents() {}

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!BodyService.isUndead(player)) return;

        event.setCanBreathe(true);
    }
}
