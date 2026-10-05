package net.alex.guzhenren.gameplay.path.qi;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.ModCommandSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path qi}: writes Qi [气] holdings; {@code /gzr info} reads them.
 *
 * <p>Builds the Qi kind and its {@code set}/{@code add}/{@code sub} through
 * {@link ModCommandSupport#enumCounter}. All writes delegate to
 * {@link PathQiService}.
 *
 * <p>⚠ A holding is a time anchor, so an amount written here begins decaying at once. Reading it back
 * a moment later and finding it smaller is correct behavior.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdQi {

    private CmdQi() {}

    private static final String ARG_KIND = "kind";

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("qi").then(ModCommandSupport.enumCounter(ARG_KIND, QiKind.values(),
                LongArgumentType.longArg(), PathQiService::set, PathQiService::add));
    }
}
