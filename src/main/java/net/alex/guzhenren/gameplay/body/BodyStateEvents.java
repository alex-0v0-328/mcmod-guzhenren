package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reacts to the vanilla moments that change body [肉身] behavior: {@code onBreathe} lets an undead life
 * form breathe underwater, {@code onIncomingDamage} hands a hit to the worn skin [皮肤] for its flat reduction and
 * cancels a hit the skin turned whole, so it leaves no knockback, hurt sound or invulnerability frames behind.
 * The attack refreshes on effect changes and attacks live in {@code gameplay.attribute.AttributeEvents}.
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

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        float reduced = SkinService.reduceDamage(player, event.getSource(), event.getAmount());
        if (reduced <= 0.0F && event.getAmount() > 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(reduced);
    }
}
