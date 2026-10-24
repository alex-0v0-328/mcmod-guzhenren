package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

/**
 * Sets Epic Fight's per-level skill-retention rule for every loaded server level, re-reads the stamina
 * modifier whenever the apertures change ({@code ApertureChangedEvent}) or the lifecycle rebuilds a player's
 * derived attributes ({@code PlayerRefreshedEvent}), and opens the punch shockwave
 * trail on a heavy fist landing.
 *
 * <p>{@link #onIncomingDamage}: a landed attack from an Epic-Fight-mode player punching bare-handed
 * (or with a fist-category weapon; an empty hand resolves to FIST too) opens the punch shockwave
 * trail at the struck target's hitbox center -- rings planting along the punch ray behind the
 * target, each blooming small-to-large in place -- when the attack panel has reached
 * {@link AttackDamageService#IMPACT_RING_ATTACK_THRESHOLD}. The panel is read as-is, so weapon damage
 * only rides along for fist-category items.
 *
 * <p>⚠ A version seam: the NeoForge 1.21.1 mainline hooks {@code LivingIncomingDamageEvent}; this port hooks
 * Forge's {@code LivingAttackEvent}, which fires at the same point of {@code LivingEntity.hurt}, after the
 * invulnerability checks and before any damage processing.
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EpicFightServerEvents {

    private EpicFightServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            EpicFightGameRules.KEEP_SKILLS.setRuleValue(level, true);
        }
    }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRefreshed(PlayerRefreshedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingAttackEvent event) {
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
