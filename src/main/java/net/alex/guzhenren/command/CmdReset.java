package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.CmdAwaken;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr reset}: puts a player back to what they were born as.
 *
 * <p>Delegates to {@link PlayerDataService#resetAll} and then calls
 * {@link net.alex.guzhenren.command.ModCommandSupport#refreshCommands}, because the reset flips the
 * awakened gate and the client must see the updated tree.
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdAwaken
 * @since 1.0.0
 */

public final class CmdReset {

    private CmdReset() {}

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return ModCommandSupport.withTargets(Commands.literal("reset"),
                context -> ModCommandSupport.apply(context, CmdReset::reset));
    }

    private static void reset(ServerPlayer player) {
        ApertureStorageMenu.closeFor(player);
        PlayerDataService.resetAll(player);
        ModCommandSupport.refreshCommands(player);
    }
}
