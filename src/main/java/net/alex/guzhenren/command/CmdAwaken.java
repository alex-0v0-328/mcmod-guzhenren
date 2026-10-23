package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.AwakenedGate;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr awaken}: opens an aperture [空窍] without the Gu that normally does it.
 *
 * <p>Sits at the root of the command tree so that {@code /gzr aperture} can carry a single
 * {@code requires()} gate. Delegates to {@link
 * ApertureService#awaken} and then calls {@link
 * net.alex.guzhenren.command.ModCommandSupport#refreshCommands} so the client sees the newly-revealed branches.
 *
 * <p>⚠ The service does not refuse a holder who is already awakened; it appends another one. The gate
 * has to live here, in the caller.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.CmdReset
 * @since 1.0.0
 */

public final class CmdAwaken {

    private CmdAwaken() {}

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return ModCommandSupport.withTargets(Commands.literal("awaken"),
                context -> ModCommandSupport.applyIf(context, AwakenedGate.AWAKENED.negate(),
                        ModCommandSupport.FAILED_AWAKENED, CmdAwaken::awaken));
    }

    private static void awaken(ServerPlayer player) {
        ApertureStorageMenu.closeFor(player);
        ApertureService.awaken(player);
        ModCommandSupport.refreshCommands(player);
    }
}
