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
 * 接收单个可序列化枚举常量的命令参数构建与解析工具（底层使用 {@link StringArgumentType#word()}）。
 * <ul>
 *   <li>{@link #createArgument(String, Enum[])} 提供静态枚举候选补全，{@link #createArgument(String, Function)} 则根据
 *       当前已解析的上下文动态计算候选枚举列表。</li>
 *   <li>⚠ 动态候选函数必须容忍前置参数为任意非法字符串，因为底层 {@code word()} 在补全阶段会接收玩家输入的任意单
 *       词。</li>
 *   <li>⚠ 同一条命令链上的两个枚举参数必须使用不同的参数名（如将 {@code ARG_PATH} 或 {@code ARG_KIND} 与
 *       {@link ModCommandSupport#ARG_VALUE} 区分开），否则 Brigadier 上下文会以后一个同名参数静默覆盖前一个参数。</li>
 *   <li>⚠ {@link #get} 必须从 {@link CommandContext#getLastChild()} 最深层子上下文读取参数值：当玩家通过重定向别名
 *       （{@code /gzr}）输入指令时，重定向后的参数全部解析在子上下文中，而补全回调收到的却是根上下文；若直接读根上下文
 *       会抛出 {@link IllegalArgumentException} 并导致 {@code /gzr} 下补全静默失效。</li>
 * </ul>
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
