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
 * {@code /gzr path}: writes attainment [造诣] and the bare Dao marks [道痕], takes path effects [流派效果] off, and
 * carries the qi and strength sub-commands. Uses {@link net.alex.guzhenren.command.ModEnumArgument} for the path
 * argument; all writes delegate to {@link PathService} and {@link PathEffectService}.
 *
 * <p>{@code marks <path> set|add|sub <marks> [strands set|add|sub <strands>]} writes the path's bare marks on the
 * body [肉体], the only carrier a mark without an effect has: whole marks [条], then optionally strands [丝] in the same
 * line; {@code marks <path> strands set|add|sub <strands>} writes strands alone. The two counters never convert.
 * {@code effect remove <type>} takes every effect of that type off, its marks with it; {@code effect clear} takes
 * them all.
 *
 * <p>☠ A command writes bare marks and can put no effect on here: an effect's marks come only with the command
 * that sets what lays it down -- a race, a physique, a grade -- so they carry that source and are revoked with it;
 * a handwritten effect could not be told from one a source laid down, and nothing would ever revoke it.
 *
 * <p>☠ The literals {@code marks}, {@code attainment} and {@code effect} come before the path word and the verbs
 * after it ({@code /gzr path marks <path> set ...}): {@code GuPath} also has {@code qi}/{@code strength}
 * constants, and a word argument directly under {@code path}, beside the {@code qi}/{@code strength} literals, could
 * never be typed as those two paths.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdPath {

    private CmdPath() {}

    private static final String ARG_PATH = "path";
    private static final String ARG_TYPE = "type";
    private static final String ARG_STRANDS = "strands";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("path")
                .then(buildMarks())
                .then(buildAttainment())
                .then(buildEffect())
                .then(CmdQi.buildNode())
                .then(CmdStrength.buildNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildMarks() {
        RequiredArgumentBuilder<CommandSourceStack, String> path = ModEnumArgument.createArgument(ARG_PATH,
                GuPath.values());
        for (Write marks : Write.values()) {
            path.then(Commands.literal(marks.getWord()).then(ModCommandSupport.withTargets(
                    Commands.argument(ModCommandSupport.ARG_VALUE, LongArgumentType.longArg(0L)),
                    context -> writeBare(context, marks, null)).then(buildStrands(marks))));
        }
        return Commands.literal("marks").then(path.then(buildStrands(null)));
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

    //region builders
    private static ArgumentBuilder<CommandSourceStack, ?> buildStrands(@Nullable Write marks) {
        ArgumentBuilder<CommandSourceStack, ?> strands = Commands.literal(ARG_STRANDS);
        for (Write write : Write.values()) {
            strands.then(Commands.literal(write.getWord()).then(ModCommandSupport.withTargets(
                    Commands.argument(ARG_STRANDS, LongArgumentType.longArg(0L)),
                    context -> writeBare(context, marks, write))));
        }
        return strands;
    }

    private static int writeBare(CommandContext<CommandSourceStack> context, @Nullable Write marks,
                                 @Nullable Write strands) throws CommandSyntaxException {
        GuPath path = getPathOf(context);
        long markValue = marks == null ? 0L : LongArgumentType.getLong(context, ModCommandSupport.ARG_VALUE);
        long strandValue = strands == null ? 0L : LongArgumentType.getLong(context, ARG_STRANDS);
        return ModCommandSupport.apply(context, player -> {
            if (marks != null) {
                PathService.setBareMarks(player, path, marks.apply(PathService.getBareMarks(player, path).marks(),
                        markValue));
            }
            if (strands != null) {
                PathService.setBareStrands(player, path, strands.apply(
                        PathService.getBareMarks(player, path).strands(), strandValue));
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

        String getWord() { return name().toLowerCase(Locale.ROOT); }

        long apply(long current, long amount) {
            return switch (this) {
                case SET -> amount;
                case ADD -> LongMath.saturatedAdd(current, amount);
                case SUB -> LongMath.saturatedSubtract(current, amount);
            };
        }
    }
}
