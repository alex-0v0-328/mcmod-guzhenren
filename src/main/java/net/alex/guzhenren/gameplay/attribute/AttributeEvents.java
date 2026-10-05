package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.PhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.path.strength.StrengthChangedEvent;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Every moment the two attribute modifiers are recomputed outside the heartbeat. The mod's own writers
 * post {@link PhysiqueChangedEvent}, {@link StrengthChangedEvent} and {@link ApertureChangedEvent};
 * vanilla's effect events and {@code onAttack} cover the rest. {@code onAttack} refreshes the
 * health-based Hardship Strength Gu capacity before vanilla reads attack damage.
 *
 * <p>⚠ Left to the heartbeat alone the attack row would lag by up to a second, which is visible on
 * the panel.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @see MaxHealthService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AttributeEvents {

    private AttributeEvents() {}

    @SubscribeEvent
    public static void onPhysiqueChanged(PhysiqueChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onStrengthChanged(StrengthChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { MaxHealthService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU)) AttackDamageService.refresh(player);
    }

    private static void refreshAttack(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) AttackDamageService.refresh(player);
    }
}
