package net.alex.guzhenren.command;

import com.google.common.math.LongMath;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /gzr path} 子命令：修改各流派造诣与肉身裸道痕[Path Marks]、移除流派效果，并挂载力道子命令
 * （{@link CmdStrength}）。
 * <ul>
 *   <li>流派参数通过 {@link ModEnumArgument} 解析，数据修改分别委托给 {@link PathService} 与
 *       {@link PathEffectService}。</li>
 *   <li>{@code marks <path> set|add|sub <marks> [points set|add|sub <points>]} 先写该流派的肉身裸道痕条数、再可选写点
 *       数，{@code marks <path> points set|add|sub <points>} 则单独写点数；条与点互不自动换算。</li>
 *   <li>{@code effect remove <type>} 移除指定类型的全部流派效果及其附带道痕，{@code effect clear} 清空全部流派效
 *       果。</li>
 *   <li>⚠ 本命令仅允许直接修改肉身裸道痕，不提供手动挂载流派效果的入口：效果道痕必须由设置种族、体质或肉身品级等来源命
 *       令自动挂载并随来源一同撤销，若手动绕过来源注入效果将导致该效果永远无法被正常卸载。</li>
 *   <li>⚠ 子分支字面量 {@code marks}、{@code attainment}、{@code effect} 必须置于流派参数之前：因为 {@link GuPath} 枚举
 *       本身包含 {@code strength}，若将流派参数直接挂在 {@code path} 下与 {@code strength} 字面量子命令并列，会导致力道
 *       枚举值被字面量节点遮蔽而无法输入。</li>
 * </ul>
 */

public final class CmdPath {

    private CmdPath() {}

    private static final String ARG_PATH = "path";
    private static final String ARG_TYPE = "type";
    private static final String ARG_POINTS = "points";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("path")
                .then(buildMarks())
                .then(buildAttainment())
                .then(buildEffect())
                .then(CmdStrength.buildNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildMarks() {
        RequiredArgumentBuilder<CommandSourceStack, String> path = ModEnumArgument.createArgument(ARG_PATH,
                GuPath.values());
        for (Write marks : Write.values()) {
            path.then(Commands.literal(marks.getWord()).then(ModCommandSupport.withTargets(
                    Commands.argument(ModCommandSupport.ARG_VALUE, LongArgumentType.longArg(0L)),
                    context -> writeBare(context, marks, null)).then(buildPoints(marks))));
        }
        return Commands.literal("marks").then(path.then(buildPoints(null)));
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

    private static ArgumentBuilder<CommandSourceStack, ?> buildEffect() {
        return Commands.literal("effect")
                .then(Commands.literal("remove").then(ModCommandSupport.withTargets(
                        Commands.argument(ARG_TYPE, ResourceLocationArgument.id()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggestResource(
                                        ModPathEffectTypes.getAll().stream().map(PathEffectType::getId), builder)),
                        CmdPath::removeType)))
                .then(ModCommandSupport.withTargets(Commands.literal("clear"),
                        context -> ModCommandSupport.apply(context,
                                player -> PathEffectService.remove(player, effect -> true))));
    }

    //region builders [构造]
    private static ArgumentBuilder<CommandSourceStack, ?> buildPoints(@Nullable Write marks) {
        ArgumentBuilder<CommandSourceStack, ?> points = Commands.literal(ARG_POINTS);
        for (Write write : Write.values()) {
            points.then(Commands.literal(write.getWord()).then(ModCommandSupport.withTargets(
                    Commands.argument(ARG_POINTS, LongArgumentType.longArg(0L)),
                    context -> writeBare(context, marks, write))));
        }
        return points;
    }

    private static int writeBare(CommandContext<CommandSourceStack> context, @Nullable Write marks,
                                 @Nullable Write points) throws CommandSyntaxException {
        GuPath path = getPathOf(context);
        long markValue = marks == null ? 0L : LongArgumentType.getLong(context, ModCommandSupport.ARG_VALUE);
        long pointValue = points == null ? 0L : LongArgumentType.getLong(context, ARG_POINTS);
        return ModCommandSupport.apply(context, player -> {
            if (marks != null) {
                PathService.setBareMarks(player, path, marks.apply(PathService.getBareMarks(player, path).marks(),
                        markValue));
            }
            if (points != null) {
                PathService.setBarePoints(player, path, points.apply(
                        PathService.getBareMarks(player, path).points(), pointValue));
            }
        });
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildAttainmentShift(String literal, int delta) {
        return ModCommandSupport.withTargets(Commands.literal(literal), context -> {
            GuPath path = getPathOf(context);
            return ModCommandSupport.apply(context, player -> PathService.shiftAttainment(player, path, delta));
        });
    }

    private static int removeType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, ARG_TYPE);
        PathEffectType<?> type = ModPathEffectTypes.get(id);
        if (type == null) {
            ModCommandFeedback.sendFailure(context.getSource(),
                    Component.translatable("guzhenren.command.unknown_value", id.toString()));
            return 0;
        }
        return ModCommandSupport.apply(context, player -> PathEffectService.removeType(player, type));
    }

    private static GuPath getPathOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ModEnumArgument.get(context, ARG_PATH, GuPath.values());
    }
    //endregion

    private enum Write {

        SET, ADD, SUB;

        private String getWord() { return name().toLowerCase(Locale.ROOT); }

        long apply(long current, long amount) {
            return switch (this) {
                case SET -> amount;
                case ADD -> LongMath.saturatedAdd(current, amount);
                case SUB -> LongMath.saturatedSubtract(current, amount);
            };
        }
    }
}
