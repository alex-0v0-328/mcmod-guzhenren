package net.alex.guzhenren.gameplay.dimension;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Hands each interaction inside the Treasure Yellow Heaven [宝黄天] to {@link TreasureYellowHeavenGuard} and
 * cancels what it refuses. Loader glue only: the events differ between the NeoForge 1.21.1 mainline and the
 * Forge 1.20.1 port, the rules behind them do not.
 *
 * @author Alex
 * @version 1.0.0
 * @see TreasureYellowHeavenGuard
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class TreasureYellowHeavenGuardEvents {

    private TreasureYellowHeavenGuardEvents() {}

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (TreasureYellowHeavenGuard.refusesBreak(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (TreasureYellowHeavenGuard.refusesPlace(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (TreasureYellowHeavenGuard.refusesUse(event.getEntity(), null)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (TreasureYellowHeavenGuard.refusesUse(event.getEntity(), null)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (TreasureYellowHeavenGuard.refusesUse(event.getEntity(), event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (TreasureYellowHeavenGuard.refusesUse(event.getEntity(), event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (TreasureYellowHeavenGuard.refusesAttack(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (TreasureYellowHeavenGuard.takesBackToss(event.getPlayer(), event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (TreasureYellowHeavenGuard.refusesDamage(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) TreasureYellowHeavenGuard.tick(player);
    }
}
