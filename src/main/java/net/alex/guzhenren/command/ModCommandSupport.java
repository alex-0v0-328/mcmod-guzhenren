package net.alex.guzhenren.command;

import com.google.common.math.LongMath;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * The shared pieces every subcommand is built from: target resolution, gating, and feedback. Provides
 * the {@code withTargets} wrapper hanging {@code [targets]} off a literal, the {@code apply}/{@code
 * applyIf} runners iterating per target (one loop, in {@code applyIfResult}), and {@code refreshCommands}
 * re-sending the command tree after a gate flip; also the
 * shared verb builders ({@code enumSetNode}, {@code longNode}, {@code counter}, {@code enumCounter}).
 *
 * <p>{@code enumCounter} is the set/add/sub triple behind one enum argument; the amount is read as
 * any {@code Number} so an {@code int} argument narrows back exactly. Its {@code sub} negates plainly,
 * unlike {@code counter}, which saturates.
 *
 * <p>⚠ Anything that flips the answer of a {@code requires()} predicate has to ask this class to
 * refresh the command tree, or the client keeps the tree it was last sent.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModCommandFeedback
 * @since 1.0.0
 */

public final class ModCommandSupport {

    private ModCommandSupport() {}

    public static final String ARG_TARGETS = "targets";
    public static final String ARG_VALUE = "value";
    public static final String FAILED_AWAKENED = "guzhenren.command.failed.awakened";
    public static final String FAILED_UNAWAKENED = "guzhenren.command.failed.unawakened";
    public static final String FAILED_EXTREME = "guzhenren.command.failed.extreme_physique_required";
    public static final Predicate<ServerPlayer> ANYONE = player -> true;
    public static void refreshCommands(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) server.getCommands().sendCommands(player);
    }

    //region node builders
    public static ArgumentBuilder<CommandSourceStack, ?> withTargets(
            ArgumentBuilder<CommandSourceStack, ?> node, Command<CommandSourceStack> executor) {
        return node.executes(executor)
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players()).executes(executor));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> longNode(String literal, LongOperation operation) {
        return longNode(literal, operation, ANYONE, null);
    }

    public static ArgumentBuilder<CommandSourceStack, ?> longNode(
            String literal, LongOperation operation, Predicate<ServerPlayer> allowed, String refusedKey) {
        return Commands.literal(literal).then(withTargets(
                Commands.argument(ARG_VALUE, LongArgumentType.longArg()),
                context -> {
                    long value = LongArgumentType.getLong(context, ARG_VALUE);
                    return applyIf(context, allowed, refusedKey, player -> operation.apply(player, value));
                }));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> counter(
            String literal, LongOperation set, LongOperation add) {
        return Commands.literal(literal)
                .then(longNode("set", set))
                .then(longNode("add", add))
                .then(longNode("sub", (player, value) -> add.apply(player, LongMath.saturatedSubtract(0L, value))));
    }

    public static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> enumCounter(
            String argument, E[] values, ArgumentType<? extends Number> amountType,
            EnumCountOperation<E> set, EnumCountOperation<E> add) {
        return ModEnumArgument.arg(argument, values)
                .then(enumCountNode("set", argument, values, amountType, set))
                .then(enumCountNode("add", argument, values, amountType, add))
                .then(enumCountNode("sub", argument, values, amountType,
                        (player, value, amount) -> add.apply(player, value, -amount)));
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> enumCountNode(
            String literal, String argument, E[] values, ArgumentType<? extends Number> amountType,
            EnumCountOperation<E> operation) {
        return Commands.literal(literal).then(withTargets(Commands.argument(ARG_VALUE, amountType), context -> {
            E value = ModEnumArgument.get(context, argument, values);
            long amount = context.getArgument(ARG_VALUE, Number.class).longValue();
            return apply(context, player -> operation.apply(player, value, amount));
        }));
    }

    public static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> enumSetNode(
            String literal, E[] values, EnumOperation<E> operation,
            Predicate<ServerPlayer> allowed, String refusedKey) {
        return Commands.literal(literal).then(Commands.literal("set")
                .then(withTargets(ModEnumArgument.arg(ARG_VALUE, values), context -> {
                    E value = ModEnumArgument.get(context, ARG_VALUE, values);
                    return applyIf(context, allowed, refusedKey, player -> operation.apply(player, value));
                })));
    }
    //endregion

    //region execution
    public static int apply(CommandContext<CommandSourceStack> context, PlayerOperation operation)
            throws CommandSyntaxException {
        return applyIf(context, ANYONE, null, operation);
    }

    public static int applyIf(CommandContext<CommandSourceStack> context, Predicate<ServerPlayer> allowed,
                              String refusedKey, PlayerOperation operation) throws CommandSyntaxException {
        return applyIfResult(context, allowed, refusedKey, player -> {
            operation.apply(player);
            return true;
        });
    }

    public static int applyIfResult(CommandContext<CommandSourceStack> context, Predicate<ServerPlayer> allowed,
                                    String refusedKey, ResultOperation operation) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<ServerPlayer> refused = new ArrayList<>();
        int updated = 0;

        for (ServerPlayer player : targets(context)) {
            if (!allowed.test(player) || !operation.apply(player)) {
                refused.add(player);
                continue;
            }
            updated++;
        }

        if (!refused.isEmpty()) ModCommandFeedback.failure(source, Component.translatable(refusedKey, names(refused)));
        if (updated > 0) ModCommandFeedback.success(source,
                Component.translatable("guzhenren.command.updated", updated));
        return updated;
    }

    public static Collection<ServerPlayer> targets(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        boolean explicit = context.getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals(ARG_TARGETS));

        return explicit
                ? EntityArgument.getPlayers(context, ARG_TARGETS)
                : List.of(context.getSource().getPlayerOrException());
    }

    private static Component names(List<ServerPlayer> players) {
        return ComponentUtils.formatList(players, ServerPlayer::getDisplayName);
    }
    //endregion

    @FunctionalInterface
    public interface PlayerOperation {

        void apply(ServerPlayer player);
    }

    @FunctionalInterface
    public interface ResultOperation {

        boolean apply(ServerPlayer player);
    }

    @FunctionalInterface
    public interface EnumOperation<E extends Enum<E>> {

        void apply(ServerPlayer player, E value);
    }

    @FunctionalInterface
    public interface EnumCountOperation<E extends Enum<E>> {

        void apply(ServerPlayer player, E value, long amount);
    }

    @FunctionalInterface
    public interface LongOperation {

        void apply(ServerPlayer player, long value);
    }
}
