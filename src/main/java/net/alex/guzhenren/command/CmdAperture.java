package net.alex.guzhenren.command;

import com.google.common.math.LongMath;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.UnaryOperator;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /gzr aperture}：写持有者的空窍[Aperture]，B 面板读取。
 *
 * <ul>
 *   <li>两道门槛：{@code requires(sourceAwakened)} 只管显示，逐目标门槛保护数据。</li>
 *   <li>值皆字面量，自带补全，{@code ten_extreme:verdant_great_sun} 类词无需参数类型；每词自带运算，不二次解析。</li>
 *   <li>{@code rank set rank:<one..five>|up|down} 后可接 {@code stage set stage:<init..peak>|up|down}，一并写入。</li>
 *   <li>{@code [targets]} 前可选 {@code first|second} 选空窍，默认第一空窍；不带它的 {@code essence refill}
 *       填满所有空窍真元[Essence]及精炼中空窍的精炼真元池。</li>
 *   <li>{@code talent set} 只改第一空窍：设资质[Aptitude]品级重掷基础真元、去十绝体；{@code ten_extreme:<physique>}
 *       存体质，其变更事件设满基础真元，故 {@code essence base} 停于满值下，拒绝第一空窍为十绝体者。</li>
 *   <li>⚠ {@link #getSelector} 遍历 {@code context.getNodes()} 按名匹配选窍词；同路径其他节点勿复用这两个字面量。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ModCommandSupport
 * @since 1.0.0
 */

public final class CmdAperture {

    private CmdAperture() {}

    private static final String RANK = "rank";
    private static final String STAGE = "stage";
    private static final String FIRST = "first";
    private static final String SECOND = "second";
    static final String TEN_EXTREME = "ten_extreme:";
    static final int MAX_COMMAND_BASE = Aperture.MAX_BASE - 1;
    private static final String FAILED_INDEX = "guzhenren.command.failed.aperture_index";
    private static final String FAILED_BASE_EXTREME = "guzhenren.command.failed.base_extreme";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("aperture").requires(AwakenedGate::isSourceAwakened)
                .then(buildGrade(RANK, Rank.getSettable(), Rank::shift, (word, rank) ->
                        withSelector(Commands.literal(word), context -> grade(context, rank, null))
                                .then(buildStage(rank))))
                .then(buildStage(null))
                .then(buildTalent())
                .then(buildEssence());
    }

    //region rank and stage [转数与阶段]
    private static ArgumentBuilder<CommandSourceStack, ?> buildStage(@Nullable UnaryOperator<Rank> rank) {
        return buildGrade(STAGE, Stage.getSettable(), Stage::shift,
                (word, stage) -> withSelector(Commands.literal(word), context -> grade(context, rank, stage)));
    }

    /** {@code <name> set <name>:<value>|up|down}，每个词由 {@code word} 围绕其运算构造。 */
    private static <E extends NamedEnum> ArgumentBuilder<CommandSourceStack, ?> buildGrade(
            String name, E[] settable, BiFunction<E, Integer, E> shift,
            BiFunction<String, UnaryOperator<E>, ArgumentBuilder<CommandSourceStack, ?>> word) {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        for (E value : settable) set.then(word.apply(name + ":" + value.getSerializedName(), current -> value));
        return Commands.literal(name).then(set)
                .then(word.apply("up", current -> shift.apply(current, 1)))
                .then(word.apply("down", current -> shift.apply(current, -1)));
    }

    private static int grade(CommandContext<CommandSourceStack> context, @Nullable UnaryOperator<Rank> rank,
                             @Nullable UnaryOperator<Stage> stage) throws CommandSyntaxException {
        return applyOnAperture(context, (player, index) -> {
            Aperture aperture = ApertureService.getAperture(player, index);
            if (rank != null) aperture = aperture.withRank(rank.apply(aperture.rank()));
            if (stage != null) aperture = aperture.withStage(stage.apply(aperture.stage()));
            ApertureService.set(player, index, aperture);
        });
    }
    //endregion

    private static ArgumentBuilder<CommandSourceStack, ?> buildTalent() {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        for (Talent talent : Talent.getSettable()) {
            if (talent == Talent.TEN_EXTREMES) continue;
            set.then(ModCommandSupport.withTargets(Commands.literal(talent.getSerializedName()),
                    context -> AwakenedGate.applyOnAwakened(context, player -> ApertureService.setTalent(
                            player, ApertureService.get(player).getFirstIndex(), talent))));
        }
        for (ExtremePhysique physique : ExtremePhysique.getSettable()) {
            set.then(ModCommandSupport.withTargets(Commands.literal(TEN_EXTREME + physique.getSerializedName()),
                    context -> AwakenedGate.applyOnAwakened(context,
                            player -> BodyService.setExtremePhysique(player, physique))));
        }
        return Commands.literal("talent").then(set);
    }

    //region essence [真元]
    private static ArgumentBuilder<CommandSourceStack, ?> buildEssence() {
        return Commands.literal("essence")
                .then(Commands.literal("current")
                        .then(buildCurrentNode("set", (current, value) -> value))
                        .then(buildCurrentNode("add", LongMath::saturatedAdd))
                        .then(buildCurrentNode("sub", LongMath::saturatedSubtract)))
                .then(Commands.literal("base")
                        .then(buildBaseNode("set", (base, value) -> value))
                        .then(buildBaseNode("add", Integer::sum))
                        .then(buildBaseNode("sub", (base, value) -> base - value)))
                .then(withSelector(Commands.literal("refill"), context -> hasSelector(context)
                        ? applyOnAperture(context, ApertureEssenceService::refillWithDistilled)
                        : AwakenedGate.applyOnAwakened(context, ApertureEssenceService::refillWithDistilled)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildCurrentNode(String literal, LongBinaryOperator op) {
        return Commands.literal(literal).then(withSelector(
                Commands.argument(ModCommandSupport.ARG_VALUE, LongArgumentType.longArg(0L)), context -> {
                    long value = LongArgumentType.getLong(context, ModCommandSupport.ARG_VALUE);
                    return applyOnAperture(context, (player, index) -> ApertureEssenceService.set(player, index,
                            op.applyAsLong(ApertureService.getAperture(player, index).currentEssence(), value)));
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildBaseNode(String literal, IntBinaryOperator op) {
        return Commands.literal(literal).then(withSelector(
                Commands.argument(ModCommandSupport.ARG_VALUE, IntegerArgumentType.integer(0, MAX_COMMAND_BASE)),
                context -> {
                    int value = IntegerArgumentType.getInteger(context, ModCommandSupport.ARG_VALUE);
                    boolean second = isSecond(context);
                    return ModCommandSupport.applyIfResult(context,
                            player -> getIndex(player, second) >= 0 && (second || !BodyService.isExtreme(player)),
                            second ? FAILED_INDEX : FAILED_BASE_EXTREME,
                            player -> {
                                int index = getIndex(player, second);
                                int base = ApertureService.getAperture(player, index).baseEssence();
                                return ApertureService.setBaseEssence(player, index,
                                        Mth.clamp(op.applyAsInt(base, value), Aperture.MIN_BASE, MAX_COMMAND_BASE));
                            });
                }));
    }
    //endregion

    //region aperture selection [选窍]
    private static ArgumentBuilder<CommandSourceStack, ?> withSelector(
            ArgumentBuilder<CommandSourceStack, ?> node, Command<CommandSourceStack> executor) {
        return ModCommandSupport.withTargets(node, executor)
                .then(ModCommandSupport.withTargets(Commands.literal(FIRST), executor))
                .then(ModCommandSupport.withTargets(Commands.literal(SECOND), executor));
    }

    private static int applyOnAperture(CommandContext<CommandSourceStack> context, Indexed operation)
            throws CommandSyntaxException {
        boolean second = isSecond(context);
        return ModCommandSupport.applyIf(context, player -> getIndex(player, second) >= 0,
                second ? FAILED_INDEX : ModCommandSupport.FAILED_UNAWAKENED,
                player -> operation.apply(player, getIndex(player, second)));
    }

    private static int getIndex(ServerPlayer player, boolean second) {
        ApertureData data = ApertureService.get(player);
        return second ? data.getSecondIndex() : data.getFirstIndex();
    }

    private static boolean hasSelector(CommandContext<CommandSourceStack> context) {
        return getSelector(context) != null;
    }

    private static boolean isSecond(CommandContext<CommandSourceStack> context) {
        return SECOND.equals(getSelector(context));
    }

    static @Nullable String getSelector(CommandContext<CommandSourceStack> context) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = context.getNodes();
        for (int i = 1; i < nodes.size(); i++) {
            String current = nodes.get(i).getNode().getName();
            if (current.equals(FIRST) || current.equals(SECOND)) return current;
        }
        return null;
    }
    //endregion

    @FunctionalInterface
    private interface Indexed {

        void apply(ServerPlayer player, int aperture);
    }
}
