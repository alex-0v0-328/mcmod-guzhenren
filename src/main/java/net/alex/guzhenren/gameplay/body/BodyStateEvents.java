package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reacts to the vanilla moments that change body [肉身] behavior: {@code onBreathe} lets an undead life
 * form breathe underwater, {@code onIncomingDamage} hands a hit to the worn skin [皮肤] for its flat reduction and
 * cancels a hit the skin turns whole, so it leaves no knockback, hurt sound or invulnerability frames behind; what
 * passes meets the Heavenly Mugwort shield [天蓬护盾] after the invulnerability frames and before armor, through
 * the hit's armor reduction, so a hit the shield takes whole still starts the frames as vanilla Absorption does.
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

        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        float reduced = SkinService.reduceDamage(player, event.getSource(), amount);
        if (reduced <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(reduced);
        if (player instanceof ServerPlayer server && MugwortShieldService.isUp(server)) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR,
                    (container, armor) -> shieldThenArmor(server, container, armor));
        }
    }

    /** The armor step once the shield has taken its share: armor then works only on what the shield passed. */
    private static float shieldThenArmor(ServerPlayer player, DamageContainer container, float armor) {
        DamageSource source = container.getSource();
        float landing = container.getNewDamage();
        float rest = MugwortShieldService.absorb(player, source, landing);
        if (rest >= landing) return armor;
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return landing - rest;
        float armored = CombatRules.getDamageAfterAbsorb(player, rest, source, player.getArmorValue(),
                (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        return landing - armored;
    }
}
