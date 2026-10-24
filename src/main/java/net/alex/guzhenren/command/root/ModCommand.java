package net.alex.guzhenren.command.root;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.command.AwakenedGate;
import net.alex.guzhenren.command.CmdAperture;
import net.alex.guzhenren.command.CmdAwaken;
import net.alex.guzhenren.command.CmdBody;
import net.alex.guzhenren.command.CmdMind;
import net.alex.guzhenren.command.CmdPath;
import net.alex.guzhenren.command.CmdReset;
import net.alex.guzhenren.command.CmdSoul;
import net.alex.guzhenren.command.CmdTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The root of {@code /guzhenren} (alias {@code /gzr}), assembling every subcommand under it.
 *
 * <p>Registers the literal {@code guzhenren} with permission level 2, then attaches the eight root
 * branches ({@link net.alex.guzhenren.command.root.CmdInfo}, {@code CmdAwaken}, {@code CmdReset},
 * {@code CmdAperture}, {@code CmdBody}, {@code CmdSoul}, {@code CmdPath}, {@code CmdMind}) -- all
 * attachment-data commands. The
 * {@code gzr} alias is a {@code redirect} to that root, so everything typed after it parses into a
 * child context.
 *
 * <p>World-environment commands live on the separate {@code guworld} root (same permission level):
 * currently {@link net.alex.guzhenren.command.CmdTravel}'s {@code enter}/{@code exit}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModCommand {

    private ModCommand() {}

    private static final int PERMISSION_LEVEL = 2;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("guzhenren")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .then(CmdInfo.buildNode())
                        .then(CmdAwaken.buildNode())
                        .then(CmdReset.buildNode())
                        .then(CmdAperture.buildNode())
                        .then(CmdBody.buildNode(AwakenedGate.AWAKENED))
                        .then(CmdSoul.buildNode())
                        .then(CmdPath.buildNode())
                        .then(CmdMind.buildNode()));

        dispatcher.register(Commands.literal("gzr")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .redirect(root));

        dispatcher.register(Commands.literal("guworld")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(CmdTravel.enterNode())
                .then(CmdTravel.exitNode()));
    }
}
