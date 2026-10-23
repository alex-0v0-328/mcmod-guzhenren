package net.alex.guzhenren.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * One translated line on a player's action bar, the mod's voice for a use, a menu click or a heartbeat step:
 * {@link #refuse} in red for what did not happen, {@link #inform} in the default color for what did.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ActionBar {

    private ActionBar() {}

    public static void refuse(@NotNull Player player, @NotNull String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }

    public static void inform(@NotNull Player player, @NotNull String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
