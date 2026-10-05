package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.jetbrains.annotations.Nullable;

/**
 * The long cooldowns a tended Gu stamps on its own stack: vanilla draws the sweep, the stack's stamp (an
 * overworld game time) is the truth. {@link #cooldownStamp} writes one, {@link #cooldownLeft} reads it back.
 *
 * <p>⚠ {@link #cooldownStamp}: stamped BACK by the share a hastened clock has already served, because the
 * stamp is read again long after the form has ended -- scaling the window on the way out would recompute a
 * live cooldown.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

final class GuCooldowns {

    private GuCooldowns() {}

    static long cooldownStamp(ServerPlayer player, int window) {
        long gameTime = player.server.overworld().getGameTime();
        return gameTime - (window - PathTimeFlowService.shortenWait(player, window));
    }

    static int stampCooldownLeft(long now, @Nullable Long stamp, int window) {
        if (stamp == null || window <= 0) return 0;

        long elapsed = now - stamp;
        return elapsed < 0L || elapsed >= window ? 0 : (int) (window - elapsed);
    }

    static int cooldownLeft(Player player, @Nullable Long stamp, int window) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : stampCooldownLeft(server.overworld().getGameTime(), stamp, window);
    }

    static void applyPostRefineCooldown(ItemCooldowns cooldowns, Item item, int wanted) {
        if (!cooldowns.isOnCooldown(item)) cooldowns.addCooldown(item, wanted);
    }
}
