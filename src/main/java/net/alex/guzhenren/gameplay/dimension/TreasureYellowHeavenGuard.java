package net.alex.guzhenren.gameplay.dimension;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.TossedItems;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authority rules of the Treasure Yellow Heaven [宝黄天] anchored dimension. While inside, a player may
 * only move, fly, chat, and use the exit command; every other interaction is refused. Void damage is refused too,
 * so {@link #tick} can return the player to spawn.
 *
 * <p>Break, place, use and attack answer the same on both sides, since the client knows its own dimension: a
 * refusal there stops the client predicting the interaction (no ghost block, no swing, no use animation started),
 * and the server's own answer stays the authority, since the client still sends the packet. Toss and damage are
 * server-side only; the toss is the one check that tells the player something, so it runs exactly once.
 *
 * <p>In {@link #tick}, leaving by any route -- exit, vanilla {@code /tp}, another mod's teleport, a respawn
 * after dying inside -- drops the flight grant, unless the player stands in another anchored dimension that grants
 * flight, and drops the {@link TossStashData} at the player's feet.
 *
 * <p>The one entity a player may right-click here is a {@link SoulTraderEntity}: trading is what this heaven is
 * for. Attacks on it stay refused like every other attack. {@link #isUseRefused} takes the clicked entity, or
 * {@code null} for a right click on a block or with an item.
 *
 * <p>⚠ No toss happens here: {@link #stowOrStash} puts it back through {@link TossedItems#putBack}, which never
 * drops, and stashes what does not fit for the way out, telling the player the bag is full. No item entity ever
 * enters this heaven, so nothing hovers or despawns. {@link #stowOrStash} and {@link #dropStash} skip the
 * dimension check, since a GameTest world has no live Treasure Yellow Heaven.
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

    public static boolean isInside(Entity entity) {
        return entity.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    public static boolean isSoulTrader(Entity target) {
        return target instanceof SoulTraderEntity;
    }

    public static boolean isBreakRefused(Player player) { return isInside(player); }

    public static boolean isPlaceRefused(@Nullable Entity placer) {
        return placer instanceof Player player && isInside(player);
    }

    public static boolean isUseRefused(Player player, @Nullable Entity target) {
        return isInside(player) && !isSoulTrader(target);
    }

    public static boolean isAttackRefused(Player player) { return isInside(player); }

    public static boolean tryStowToss(Player player, ItemEntity tossed) {
        if (player.level().isClientSide() || !isInside(player)) return false;
        stowOrStash(player, tossed);
        return true;
    }

    public static void stowOrStash(Player player, ItemEntity tossed) {
        ItemStack rest = TossedItems.putBack(player, tossed);
        if (rest.isEmpty()) return;

        PlayerData data = PlayerData.get(player);
        data.setTossStash(data.getTossStash().with(rest));
        ActionBar.refuse(player, INVENTORY_FULL);
    }

    public static void dropStash(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        TossStashData stash = data.getTossStash();
        if (stash.isEmpty()) return;

        data.setTossStash(TossStashData.DEFAULT);
        for (ItemStack stack : stash.stacks()) player.spawnAtLocation(stack.copy());
    }

    public static boolean isDamageRefused(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return false;
        if (source.getEntity() instanceof Player attacker && isInside(attacker)) return true;
        return victim instanceof Player && isInside(victim) && source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    public static void tick(ServerPlayer player) {
        if (!isInside(player)) {
            if (!ModDimensions.isFlightGranted(player.level().dimension())) {
                DimensionTravelService.revokeFlight(player);
            }
            dropStash(player);
            return;
        }
        DimensionTravelService.ensureFlight(player);
        DimensionTravelService.rescueIfBelowVoid(player, ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN);
    }
}
