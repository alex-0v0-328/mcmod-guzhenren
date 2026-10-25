package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.MarkTag;
import net.alex.guzhenren.gameplay.path.PathService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path}: writes attainment [造诣] and marks [道痕], and carries the qi and strength
 * sub-commands. Uses {@link net.alex.guzhenren.command.ModEnumArgument} for the path argument; all
 * writes delegate to {@link PathService}.
 *
 * <p>☠ A command books NATURAL [自然] and can name no other tag: a handwritten source tag cannot be
 * told from what a Gu laid down, so a forged race mark would sit on a path no race ever revokes.
 *
 * <p>☠ The literals {@code marks} and {@code attainment} come before the path word and the verbs after it
 * ({@code /gzr path marks <path> set ...}): {@code GuPath} also has {@code qi}/{@code strength} constants, and a
 * word argument directly under {@code path}, beside the {@code qi}/{@code strength} literals, could never be
 * typed as those two paths.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdPath {

    private CmdPath() {}

    private static final String ARG_PATH = "path";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("path")
                .then(buildMarks())
                .then(buildAttainment())
                .then(CmdQi.buildNode())
                .then(CmdStrength.buildNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildMarks() {
        return Commands.literal("marks").then(ModCommandSupport.buildEnumCounter(ARG_PATH, GuPath.values(),
                LongArgumentType.longArg(),
                (player, path, amount) -> PathService.setMark(player, path, MarkTag.NATURAL, amount),
                (player, path, amount) -> PathService.addMark(player, path, MarkTag.NATURAL, amount)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildAttainment() {
        return Commands.literal("attainment")
                .then(ModEnumArgument.createArgument(ARG_PATH, GuPath.values())
                        .then(Commands.literal("set")
                                .then(ModCommandSupport.withTargets(
                                        ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE,
                                                GuAttainment.values()),
                                        context -> {
                                            GuPath path = getPathOf(context);
                                            GuAttainment value = ModEnumArgument.get(
                                                    context, ModCommandSupport.ARG_VALUE, GuAttainment.values());
                                            return ModCommandSupport.apply(context,
                                                    player -> PathService.setAttainment(player, path, value));
                                        })))
                        .then(buildAttainmentShift("up", 1))
                        .then(buildAttainmentShift("down", -1)));
    }

    //region builders
    private static ArgumentBuilder<CommandSourceStack, ?> buildAttainmentShift(String literal, int delta) {
        return ModCommandSupport.withTargets(Commands.literal(literal), context -> {
            GuPath path = getPathOf(context);
            return ModCommandSupport.apply(context, player -> PathService.shiftAttainment(player, path, delta));
        });
    }

    private static GuPath getPathOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ModEnumArgument.get(context, ARG_PATH, GuPath.values());
    }
    //endregion
}
