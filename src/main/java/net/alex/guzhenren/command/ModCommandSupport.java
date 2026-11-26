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
 * 模组各子命令共用的基础设施：目标玩家解析、前置条件过滤、节点构建模板与执行结果反馈。
 * <ul>
 *   <li>{@link #withTargets} 在节点末尾追加可选的 {@code [targets]} 玩家选择器（未指定时默认取命令源玩家）；
 *       {@link #apply}、{@link #applyIf} 与 {@link #applyIfResult} 调用 {@link #run} 逐目标执行操作，将目标按返回值划分
 *       为成功与拒绝两组（{@link Outcome}）并交由 {@link ModCommandFeedback} 汇总输出。</li>
 *   <li>提供三类通用节点构建器：{@link #buildEnumSetNode} 构建枚举 {@code set} 节点；{@link #buildCounter} 构建
 *       {@code long} 数值的 {@code set|add|sub} 节点（{@code sub} 使用饱和取反）；{@link #buildEnumCounter} 构建带枚举
 *       前缀的数值计数器（读取 {@link Number} 以兼容 {@code int} 精确收窄，{@code sub} 同样饱和取反，并在操作返回
 *       {@code false} 时按指定拒绝键反馈）。</li>
 *   <li>⚠ 任何改变 Brigadier 节点 {@code requires()} 判定结果的操作完成后，必须调用 {@link #refreshCommands} 向目标客
 *       户端重新下发命令树，否则客户端补全仍会保留旧命令树。</li>
 * </ul>
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
                        (player, value, amount) -> add.apply(player, value, LongMath.saturatedSubtract(0L, amount)),
                        refusedKey));
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
