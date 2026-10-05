package net.alex.guzhenren.command.root;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.AwakenedGate;
import net.alex.guzhenren.gameplay.aperture.CmdAperture;
import net.alex.guzhenren.gameplay.aperture.CmdAwaken;
import net.alex.guzhenren.gameplay.body.CmdBody;
import net.alex.guzhenren.gameplay.dimension.CmdTravel;
import net.alex.guzhenren.gameplay.lifecycle.CmdReset;
import net.alex.guzhenren.gameplay.mind.CmdMind;
import net.alex.guzhenren.gameplay.path.CmdPath;
import net.alex.guzhenren.gameplay.soul.CmdSoul;
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
 * currently {@link net.alex.guzhenren.gameplay.dimension.CmdTravel}'s {@code enter}/{@code exit}.
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
                        .then(CmdInfo.node())
                        .then(CmdAwaken.node())
                        .then(CmdReset.node())
                        .then(CmdAperture.node())
                        .then(CmdBody.node(AwakenedGate.AWAKENED))
                        .then(CmdSoul.node())
                        .then(CmdPath.node())
                        .then(CmdMind.node()));

        dispatcher.register(Commands.literal("gzr")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .redirect(root));

        dispatcher.register(Commands.literal("guworld")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(CmdTravel.enterNode())
                .then(CmdTravel.exitNode()));
    }
}
