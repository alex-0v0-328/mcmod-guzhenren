package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * A command argument accepting one constant of an enum, spelled as a {@code word()} literal. Provides
 * a static-constants factory ({@link #createArgument(String, Enum[])}) and a dynamic one
 * ({@link #createArgument(String, Function)}) computing suggestions from what was already parsed.
 *
 * <p>⚠ The {@link #createArgument(String, Function)} overload's function must tolerate a nonsense earlier
 * argument: {@code word()} accepts any word.
 *
 * <p>⚠ Two enum arguments in one chain take distinct names ({@code ARG_PATH}, {@code ARG_KIND} beside
 * {@code ModCommandSupport.ARG_VALUE}): the context keeps one argument per name, so a shared name silently
 * leaves only the later word.
 *
 * <p>☠ Everything typed after a redirect is parsed into a CHILD context, and {@code /gzr} is that
 * redirect. An executor already runs on that child, but a suggestion provider is handed the root context,
 * so its read of an earlier argument must go through {@code getLastChild()} or it throws
 * {@code IllegalArgumentException}, which Brigadier does NOT catch: the suggestion list comes back
 * empty and silent under {@code /gzr} while {@code /guzhenren} still fills it. That is why {@code get} reads
 * from the deepest context, pinned by {@code ModEnumArgumentTest}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.root.ModCommand
 * @since 1.0.0
 */

public final class ModEnumArgument {

    private ModEnumArgument() {}

    private static final DynamicCommandExceptionType UNKNOWN_VALUE = new DynamicCommandExceptionType(
            value -> Component.translatable("guzhenren.command.unknown_value", value));

    public static <E extends Enum<E> & StringRepresentable> RequiredArgumentBuilder<CommandSourceStack, String>
            createArgument(String name, E[] values) {
        return createArgument(name, context -> values);
    }

    public static <E extends Enum<E> & StringRepresentable> RequiredArgumentBuilder<CommandSourceStack, String>
            createArgument(String name, Function<CommandContext<CommandSourceStack>, E[]> offered) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(offered.apply(context)).map(StringRepresentable::getSerializedName), builder));
    }

    public static <E extends Enum<E> & StringRepresentable> E get(
            CommandContext<?> context, String name, E[] values) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context.getLastChild(), name);
        for (E value : values) {
            if (value.getSerializedName().equals(raw)) return value;
        }
        throw UNKNOWN_VALUE.create(raw);
    }
}
