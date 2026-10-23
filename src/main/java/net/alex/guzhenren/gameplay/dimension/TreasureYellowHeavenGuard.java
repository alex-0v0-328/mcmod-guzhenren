package net.alex.guzhenren.gameplay.dimension;

import net.alex.guzhenren.core.TossedItems;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authority rules of the Treasure Yellow Heaven [宝黄天] anchored dimension. While inside, a player may
 * only move, fly, chat, and use the exit command; every other interaction is refused. Void damage is refused too,
 * so {@link #tick} can return the player to spawn.
 *
 * <p>All checks are server-side. The few interactions that also reach the client answer {@code false} there, so
 * the refusal happens exactly once per interaction.
 *
 * <p>In {@link #tick}, leaving by any route -- exit, vanilla {@code /tp}, another mod's teleport -- drops the
 * flight grant, unless the player stands in another anchored dimension that grants flight.
 *
 * <p>The one entity a player may right-click here is a {@link SoulTraderEntity}: trading is what this heaven is
 * for. Attacks on it stay refused like every other attack.
 *
 * <p>⚠ A toss goes back into the bag through {@link TossedItems#stow}, which never drops; what does not fit
 * hovers where it was dropped and despawns like any dropped item, and the player is told the bag is full.
 *
 * @author Alex
 * @version 1.0.0
 * @see TreasureYellowHeavenGuardEvents
 * @see net.alex.guzhenren.gameplay.dimension.DimensionTravelService
 * @since 1.0.0
 */

public final class TreasureYellowHeavenGuard {

    private static final String INVENTORY_FULL = "guzhenren.dimension.treasure_yellow_heaven.inventory_full";

    private TreasureYellowHeavenGuard() {}

    public static boolean inside(Entity entity) {
        return entity.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    public static boolean tradesWith(Entity target) {
        return target instanceof SoulTraderEntity;
    }

    public static boolean refusesBreak(Player player) { return inside(player); }

    public static boolean refusesPlace(@Nullable Entity placer) {
        return placer instanceof Player player && !player.level().isClientSide() && inside(player);
    }

    /** A right click on a block or with an item ({@code target} null), or on an entity. */
    public static boolean refusesUse(Player player, @Nullable Entity target) {
        if (player.level().isClientSide() || !inside(player)) return false;
        return target == null || !tradesWith(target);
    }

    public static boolean refusesAttack(Player player) {
        return !player.level().isClientSide() && inside(player);
    }

    /** Stows a toss back into the bag; true when it went in and the toss should not happen. */
    public static boolean takesBackToss(Player player, ItemEntity tossed) {
        if (player.level().isClientSide() || !inside(player)) return false;
        if (TossedItems.stow(player, tossed)) return true;

        player.displayClientMessage(Component.translatable(INVENTORY_FULL).withStyle(ChatFormatting.RED), true);
        return false;
    }

    public static boolean refusesDamage(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return false;
        if (source.getEntity() instanceof Player attacker && inside(attacker)) return true;
        return victim instanceof Player && inside(victim) && source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    public static void tick(ServerPlayer player) {
        if (!inside(player)) {
            if (!ModDimensions.isFlightGranted(player.level().dimension())) {
                DimensionTravelService.revokeFlight(player);
            }
            return;
        }
        DimensionTravelService.ensureFlight(player);
        DimensionTravelService.rescueIfBelowVoid(player, ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN);
    }
}
