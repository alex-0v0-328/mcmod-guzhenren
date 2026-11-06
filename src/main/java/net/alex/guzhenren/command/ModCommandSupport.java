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
 * applyIf} runners iterating per target (one loop, in {@code run}), and {@code refreshCommands}
 * re-sending the command tree after a gate flip; also the
 * shared verb builders ({@code buildEnumSetNode}, {@code buildCounter}, {@code buildEnumCounter}).
 *
 * <p>{@code enumCounter} is the set/add/sub triple behind one enum argument; the amount is read as
 * any {@code Number} so an {@code int} argument narrows back exactly. Its {@code sub} negates plainly,
 * unlike {@code counter}, which saturates. A {@code false} write is reported under the refusal key.
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
    public static final Predicate<ServerPlayer> ANYONE = player -> true;

    public static void refreshCommands(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) server.getCommands().sendCommands(player);
    }

    //region node builders [节点构造]
    public static ArgumentBuilder<CommandSourceStack, ?> withTargets(
            ArgumentBuilder<CommandSourceStack, ?> node, Command<CommandSourceStack> executor) {
        return node.executes(executor)
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players()).executes(executor));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildLongNode(String literal, LongOperation operation) {
        return Commands.literal(literal).then(withTargets(
                Commands.argument(ARG_VALUE, LongArgumentType.longArg()),
                context -> {
                    long value = LongArgumentType.getLong(context, ARG_VALUE);
                    return apply(context, player -> operation.apply(player, value));
                }));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildCounter(
            String literal, LongOperation set, LongOperation add) {
        return Commands.literal(literal)
                .then(buildLongNode("set", set))
                .then(buildLongNode("add", add))
                .then(buildLongNode("sub",
                        (player, value) -> add.apply(player, LongMath.saturatedSubtract(0L, value))));
    }

    public static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> buildEnumCounter(
            String argument, E[] values, ArgumentType<? extends Number> amountType,
            EnumCountResultOperation<E> set, EnumCountResultOperation<E> add, String refusedKey) {
        return ModEnumArgument.createArgument(argument, values)
                .then(buildEnumCountNode("set", argument, values, amountType, set, refusedKey))
                .then(buildEnumCountNode("add", argument, values, amountType, add, refusedKey))
                .then(buildEnumCountNode("sub", argument, values, amountType,
                        (player, value, amount) -> add.apply(player, value, -amount), refusedKey));
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> buildEnumCountNode(
            String literal, String argument, E[] values, ArgumentType<? extends Number> amountType,
            EnumCountResultOperation<E> operation, String refusedKey) {
        return Commands.literal(literal).then(withTargets(Commands.argument(ARG_VALUE, amountType), context -> {
            E value = ModEnumArgument.get(context, argument, values);
            long amount = context.getArgument(ARG_VALUE, Number.class).longValue();
            return applyIfResult(context, ANYONE, refusedKey, player -> operation.apply(player, value, amount));
        }));
    }

    public static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> buildEnumSetNode(
            String literal, E[] values, EnumOperation<E> operation,
            Predicate<ServerPlayer> allowed, String refusedKey) {
        return Commands.literal(literal).then(Commands.literal("set")
                .then(withTargets(ModEnumArgument.createArgument(ARG_VALUE, values), context -> {
                    E value = ModEnumArgument.get(context, ARG_VALUE, values);
                    return applyIf(context, allowed, refusedKey, player -> operation.apply(player, value));
                })));
    }
    //endregion

    //region execution [执行]
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
        Outcome outcome = run(context, player -> allowed.test(player) && operation.apply(player));
        int updated = outcome.done().size();
        if (!outcome.refused().isEmpty()) {
            ModCommandFeedback.sendFailure(source, Component.translatable(refusedKey, outcome.refusedNames()));
        }
        if (updated > 0) ModCommandFeedback.sendSuccess(source,
                Component.translatable("guzhenren.command.updated", updated));
        return updated;
    }

    /** Runs {@code operation} on every target, splitting them by its answer. */
    static Outcome run(CommandContext<CommandSourceStack> context, ResultOperation operation)
            throws CommandSyntaxException {
        List<ServerPlayer> done = new ArrayList<>();
        List<ServerPlayer> refused = new ArrayList<>();
        for (ServerPlayer player : getTargets(context)) {
            (operation.apply(player) ? done : refused).add(player);
        }
        return new Outcome(List.copyOf(done), List.copyOf(refused));
    }

    private static Collection<ServerPlayer> getTargets(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        boolean explicit = context.getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals(ARG_TARGETS));

        return explicit
                ? EntityArgument.getPlayers(context, ARG_TARGETS)
                : List.of(context.getSource().getPlayerOrException());
    }

    //endregion

    /** The targets an operation went through for and the ones it refused, both in target order. */
    record Outcome(List<ServerPlayer> done, List<ServerPlayer> refused) {

        Component doneNames() { return getNames(done); }

        Component refusedNames() { return getNames(refused); }

        private static Component getNames(List<ServerPlayer> players) {
            return ComponentUtils.formatList(players, ServerPlayer::getDisplayName);
        }
    }

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
    public interface EnumResultOperation<E extends Enum<E>> {

        boolean apply(ServerPlayer player, E value);
    }

    @FunctionalInterface
    public interface EnumCountResultOperation<E extends Enum<E>> {

        boolean apply(ServerPlayer player, E value, long amount);
    }

    @FunctionalInterface
    public interface LongOperation {

        void apply(ServerPlayer player, long value);
    }
}
