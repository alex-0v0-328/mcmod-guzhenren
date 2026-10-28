package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reacts to the vanilla moments that change body [肉身] behavior: {@code onBreathe} lets an undead life
 * form breathe underwater, {@code onIncomingDamage} hands a hit first to the Heavenly Mugwort shield [天蓬护盾],
 * then what passes to the worn skin [皮肤] for its flat reduction, and cancels a hit the two turned whole, so it
 * leaves no knockback, hurt sound or invulnerability frames behind. Both act before armor.
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

        float shielded = player instanceof ServerPlayer server
                ? MugwortShieldService.absorb(server, event.getSource(), event.getAmount()) : event.getAmount();
        float reduced = SkinService.reduceDamage(player, event.getSource(), shielded);
        if (reduced <= 0.0F && event.getAmount() > 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(reduced);
    }
}
