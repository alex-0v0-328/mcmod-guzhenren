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
 * 接受一个枚举常量的命令参数，以 {@code word()} 字面量拼写。
 *
 * <ul>
 *   <li>{@link #createArgument(String, Enum[])} 用静态常量；{@link #createArgument(String, Function)}
 *       按已解析内容动态算补全。</li>
 *   <li>⚠ 后者的函数须容忍前面参数乱写：{@code word()} 接受任意单词。</li>
 *   <li>⚠ 同链两个枚举参数须异名（{@code ARG_PATH}、{@code ARG_KIND} 与 {@code ModCommandSupport.ARG_VALUE} 并列）：
 *       上下文每名只留一个参数，同名静默只剩后一个词。</li>
 *   <li>☠ 重定向（即 {@code /gzr}）后的输入全解析进子上下文；执行器在子上下文，补全提供器却拿到根上下文，
 *       读前面参数须经 {@code getLastChild()}，否则抛 {@code IllegalArgumentException}；Brigadier 不捕获，
 *       {@code /gzr} 下补全静默为空，{@code /guzhenren} 下照常。</li>
 *   <li>因此 {@code get} 从最深上下文读取，由 {@code ModEnumArgumentTest} 钉住。</li>
 * </ul>
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
