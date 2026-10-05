package net.alex.guzhenren.gameplay.path;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.command.ModEnumArgument;
import net.alex.guzhenren.gameplay.path.qi.CmdQi;
import net.alex.guzhenren.gameplay.path.strength.CmdStrength;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path}: writes attainment [造诣] and marks [道痕], and carries the qi and strength
 * sub-commands. Uses {@link net.alex.guzhenren.command.ModEnumArgument} for the path argument; all
 * writes delegate to {@link PathService}.
 *
 * <p>☠ A command books NATURAL [自然] and can name no other tag -- a handwritten source tag cannot be
 * told from what a Gu laid down, and that is how a race mark was forged onto a path no race revokes.
 *
 * <p>☠ The verbs sit before the path word: {@code GuPath} also has {@code qi}/{@code strength}
 * constants, so a bare word argument under {@code path} would be captured by those literals.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdPath {

    private CmdPath() {}

    private static final String ARG_PATH = "path";

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("path")
                .then(marks())
                .then(attainment())
                .then(CmdQi.node())
                .then(CmdStrength.node());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> marks() {
        return Commands.literal("marks").then(ModCommandSupport.enumCounter(ARG_PATH, GuPath.values(),
                LongArgumentType.longArg(),
                (player, path, amount) -> PathService.setMark(player, path, MarkTag.NATURAL, amount),
                (player, path, amount) -> PathService.addMark(player, path, MarkTag.NATURAL, amount)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> attainment() {
        return Commands.literal("attainment")
                .then(ModEnumArgument.arg(ARG_PATH, GuPath.values())
                        .then(Commands.literal("set")
                                .then(ModCommandSupport.withTargets(
                                        ModEnumArgument.arg(ModCommandSupport.ARG_VALUE, GuAttainment.values()),
                                        context -> {
                                            GuPath path = pathOf(context);
                                            GuAttainment value = ModEnumArgument.get(
                                                    context, ModCommandSupport.ARG_VALUE, GuAttainment.values());
                                            return ModCommandSupport.apply(context,
                                                    player -> PathService.setAttainment(player, path, value));
                                        })))
                        .then(attainmentShift("up", 1))
                        .then(attainmentShift("down", -1)));
    }

    //region builders
    private static ArgumentBuilder<CommandSourceStack, ?> attainmentShift(String literal, int delta) {
        return ModCommandSupport.withTargets(Commands.literal(literal), context -> {
            GuPath path = pathOf(context);
            return ModCommandSupport.apply(context, player -> PathService.shiftAttainment(player, path, delta));
        });
    }

    private static GuPath pathOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ModEnumArgument.get(context, ARG_PATH, GuPath.values());
    }
    //endregion
}
