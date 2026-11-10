package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeLandedEvent;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPCreateTerrainFracture;
import yesman.epicfight.registry.entries.EpicFightItems;

/**
 * Seeds Epic Fight's skill-retention rule once per world ({@link EpicFightRuleSeed}), re-reads the stamina
 * modifier whenever the apertures change ({@code ApertureChangedEvent}) or the lifecycle rebuilds a player's
 * derived attributes ({@code PlayerRefreshedEvent}), strips Epic Fight's skill books from mob drops, cracks the
 * ground under a plunge's landing, and lets go of the Store Strength Gu's [蓄力蛊] charge pose when the server drops
 * the charge on its own ({@link #onCharge}).
 *
 * <p>{@link #onPlungeLanded}: Epic Fight's own terrain fracture, the cracked ring its meteor slam leaves, at the
 * landing with the landing's impact power as its radius ({@code PlungeImpact#getImpactPower}, 2 to 6 blocks), sent to
 * every client tracking the chunk. Only the packet: {@code LevelUtil#circleSlamFracture} would also deal Epic Fight's
 * shockwave damage, and the plunge hurts nothing. The fracture is drawn on the clients alone and changes no block; it
 * goes out before the crater, since a client draws it on the ground below the landing, which the crater removes.
 * The explosion is the landing's sound, so the fracture's own stays off; its debris particles stay on.
 *
 * <p>{@link #onLivingDrops}: the loot modifier {@code guzhenren:no_skill_books} already strips skill books from chest
 * loot, but Epic Fight adds mob drops through its own global loot modifier, and NeoForge applies global loot
 * modifiers in hash order, not pack order, so ours may run first. A player's death drops keep their books: those are
 * inventory, not loot.
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
    public static void onCharge(StoreStrengthChargeEvent event) {
        if (event.getChange() == StoreStrengthChargeEvent.Change.DROPPED) {
            EpicFightIntegration.endCharge(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onPlungeLanded(PlungeLandedEvent event) {
        ServerPlayer player = event.getPlayer();
        EpicFightNetworkManager.sendToAllPlayerTrackingThisChunkWithSelf(
                new SPCreateTerrainFracture(event.getPosition(), event.getImpactPower(), true, false),
                player.serverLevel(), new ChunkPos(BlockPos.containing(event.getPosition())));
    }
}
