package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.clock.TimeFlow;
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
 * live cooldown. A slowed clock stamps FORWARD, so {@link #stampCooldownLeft} counts a stamp ahead of now as
 * long as the slowest clock ({@code TimeFlow.MIN_RATE}) could stretch the window; a stamp beyond that came from
 * another world and reads as no cooldown.
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
        return gameTime - (window - TimeFlow.shortenWait(player, window));
    }

    static int stampCooldownLeft(long now, @Nullable Long stamp, int window) {
        if (stamp == null || window <= 0) return 0;

        long left = window - (now - stamp);
        return left <= 0L || left > TimeFlow.shortenWait(TimeFlow.MIN_RATE, window) ? 0 : (int) left;
    }

    static int cooldownLeft(Player player, @Nullable Long stamp, int window) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : stampCooldownLeft(server.overworld().getGameTime(), stamp, window);
    }

    static void applyPostRefineCooldown(ItemCooldowns cooldowns, Item item, int wanted) {
        if (!cooldowns.isOnCooldown(item)) cooldowns.addCooldown(item, wanted);
    }
}
