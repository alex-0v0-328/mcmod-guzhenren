package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.body.PhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Every moment the attack modifier is recomputed outside the heartbeat. The mod's own writers
 * post {@link PhysiqueChangedEvent}, {@link ExtremePhysiqueChangedEvent} (the physique multiplies the strength
 * capacity, also for a body not yet awakened, which posts no aperture event), {@link StrengthChangedEvent},
 * {@link ApertureChangedEvent} (the rank sets the base strength capacity), {@link BodyTemperedEvent} (tendons and
 * bones move the capacity under the attack) and the skin events (a worn skin adds capacity); vanilla's effect
 * events cover the rest, and {@link LivingEquipmentChangeEvent} on the main hand moves the fist-only attack bonus
 * the moment the hand empties or fills. The max health and the knockback resistance have their own subscriber,
 * {@code gameplay.attribute.AttributeEvents}.
 *
 * <p>⚠ Left to the heartbeat alone the attack row would lag by up to a second, which is visible on
 * the panel. ⚠ NeoForge posts the three effect events BEFORE the effect map changes, so
 * {@code refreshAttack} queues the refresh as a {@link TickTask} that runs once the tick's work is done;
 * refreshing inside the event would read the effects as they were.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AttackDamageEvents {

    private AttackDamageEvents() {}

    @SubscribeEvent
    public static void onPhysiqueChanged(PhysiqueChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onExtremePhysiqueChanged(ExtremePhysiqueChangedEvent event) {
        AttackDamageService.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onStrengthChanged(StrengthChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onBodyTempered(BodyTemperedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.MAINHAND) return;
        if (event.getEntity() instanceof ServerPlayer player) AttackDamageService.refresh(player);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) { refreshAttack(event.getEntity()); }

    private static void refreshAttack(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.server.tell(new TickTask(player.server.getTickCount(), () -> AttackDamageService.refresh(player)));
    }
}
