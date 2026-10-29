package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Seeds Epic Fight's skill-retention rule once per world ({@link EpicFightRuleSeed}), re-reads the stamina
 * modifier whenever the apertures change ({@code ApertureChangedEvent}) or the lifecycle rebuilds a player's
 * derived attributes ({@code PlayerRefreshedEvent}), strips Epic Fight's skill books from mob drops, and opens the
 * punch shockwave trail on a heavy fist landing.
 *
 * <p>{@link #onLivingDrops}: the loot modifier {@code guzhenren:no_skill_books} already strips skill books from chest
 * loot, but Epic Fight adds mob drops through its own global loot modifier, and NeoForge applies global loot
 * modifiers in hash order, not pack order, so ours may run first. A player's death drops keep their books: those are
 * inventory, not loot.
 *
 * <p>{@link #onIncomingDamage}: a landed attack from an Epic-Fight-mode player punching bare-handed
 * (or with a fist-category weapon; an empty hand resolves to FIST too) opens the punch shockwave
 * trail at the struck target's hitbox center -- rings planting along the punch ray behind the
 * target, each blooming small-to-large in place -- when the attack panel has reached
 * {@link AttackDamageService#IMPACT_RING_ATTACK_THRESHOLD}. The panel is read as-is, so weapon damage
 * only rides along for fist-category items.
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EpicFightServerEvents {

    private EpicFightServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { EpicFightRuleSeed.seed(event.getServer()); }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRefreshed(PlayerRefreshedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(EpicFightItems.SKILLBOOK));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || !patch.isEpicFightMode()) return;
        if (patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory()
                != CapabilityItem.WeaponCategories.FIST) return;
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null || !AttackDamageService.isImpactRingShown(attack.getValue())) return;

        RingConeEmitter.emitPunchCone(player, player.getLookAngle(),
                event.getEntity().getBoundingBox().getCenter());
    }
}
