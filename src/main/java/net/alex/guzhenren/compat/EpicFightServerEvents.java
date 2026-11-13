package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.projectile.ProjectileThrownEvent;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.ShieldRaisedEvent;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeLandedEvent;
import net.alex.guzhenren.item.gu.GuUsedEvent;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPCreateTerrainFracture;
import yesman.epicfight.registry.entries.EpicFightItems;

/**
 * Seeds Epic Fight's skill-retention rule once per world ({@link EpicFightRuleSeed}), re-reads the stamina
 * modifier whenever the apertures change ({@code ApertureChangedEvent}) or the lifecycle rebuilds a player's
 * derived attributes ({@code PlayerRefreshedEvent}), strips Epic Fight's skill books from mob drops, cracks the
 * ground under a plunge's landing and drops the plunger into the landing crouch, lets go of the Store Strength Gu's [蓄力蛊] charge pose when the server drops
 * the charge on its own ({@link #onCharge}), and throws a second-stage flier [二段飞行] back off the surface it
 * crashed into ({@link #onSecondStageCrash}: the server's crash plays the bounce clip for every client that sees the
 * flier; the flier's own client posts the event too, with a {@code LocalPlayer}, and is left to the server's).
 * Every shield [护盾] laid on a body crosses its arms ({@link #onShieldRaised}: {@code ShieldService#raise} posts the
 * event for every shield kind and every refill, so the clip is wired once here, never per Gu). Every projectile a
 * player throws by hand swings an arm ({@link #onProjectileThrown}: one {@code ProjectileThrownEvent} a Moon Blade
 * [月刃], so each blade plays one throw clip). Every landed use of a mortal Gu [凡蛊] reaches the right hand forward
 * ({@link #onGuUsed}: {@code MortalGuItem} posts {@code GuUsedEvent} from the one step every use ends in, so the
 * generic clip is wired once here and {@link EpicFightIntegration#useGu} leaves out the uses that have a clip of
 * their own). Every server player tick hands the player to
 * {@link EpicFightIntegration#tickMeditation}, which sits the player down and stands them up as nourishing and the
 * awakening ritual start and end ({@link #onPlayerTick}).
 *
 * <p>{@link #onPlungeLanded}: Epic Fight's own terrain fracture, the cracked ring its meteor slam leaves, at the
 * landing with the landing's impact power as its radius ({@code PlungeImpact#getImpactPower}, 2 to 6 blocks), sent to
 * every client tracking the chunk. Only the packet: {@code LevelUtil#circleSlamFracture} would also deal Epic Fight's
 * shockwave damage, and the plunge hurts nothing. The fracture is drawn on the clients alone and changes no block; it
 * goes out before the crater, since a client draws it on the ground below the landing, which the crater removes.
 * The explosion is the landing's sound, so the fracture's own stays off; its debris particles stay on. The same
 * landing plays the landing clip ({@link EpicFightIntegration#landPlunge}): the event is posted only for a landing
 * hard enough to show, so a drop too short to hurt plays none.
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
            EpicFightIntegration.cancelCharge(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onSecondStageCrash(SecondStageCrashEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) EpicFightIntegration.crashFlight(player);
    }

    @SubscribeEvent
    public static void onShieldRaised(ShieldRaisedEvent event) {
        EpicFightIntegration.raiseShield(event.getPlayer());
    }

    @SubscribeEvent
    public static void onProjectileThrown(ProjectileThrownEvent event) {
        EpicFightIntegration.throwProjectile(event.getPlayer());
    }

    @SubscribeEvent
    public static void onGuUsed(GuUsedEvent event) { EpicFightIntegration.useGu(event.getPlayer()); }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) EpicFightIntegration.tickMeditation(player);
    }

    @SubscribeEvent
    public static void onPlungeLanded(PlungeLandedEvent event) {
        ServerPlayer player = event.getPlayer();
        EpicFightNetworkManager.sendToAllPlayerTrackingThisChunkWithSelf(
                new SPCreateTerrainFracture(event.getPosition(), event.getImpactPower(), true, false),
                player.serverLevel(), new ChunkPos(BlockPos.containing(event.getPosition())));
        EpicFightIntegration.landPlunge(player);
    }
}
