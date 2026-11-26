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
 * {@code /gzr aperture} 子命令：修改目标玩家的空窍状态（转数、阶段、资质、真元），结果由客户端 B 面板读取展示。
 * <ul>
 *   <li>根节点通过 {@link AwakenedGate#isSourceAwakened} 控制命令树可见性，执行期再按目标玩家与所选空窍做存在性校
 *       验。</li>
 *   <li>枚举选项统一注册为字面量节点（如 {@code ten_extreme:verdant_great_sun}）以直接提供补全并绑定对应写操作；
 *       {@link #buildGrade} 构建 {@code <name> set <name>:<value>|up|down} 节点，且 {@code rank} 分支后可继续串联
 *       {@code stage} 分支一次写入。</li>
 *   <li>目标参数 {@code [targets]} 前可加 {@code first} 或 {@code second} 选择第一或第二空窍（省略时默认操作第一空
 *       窍）；不带选窍字面量的 {@code essence refill} 则会一次性填满目标玩家所有空窍的当前真元及精炼真元池。</li>
 *   <li>{@code talent set} 仅作用于第一空窍：设置常规资质[Aptitude]会重掷基础真元并清除十绝体，设置
 *       {@code ten_extreme:<physique>} 则写入十绝体并通过事件将第一空窍基础真元锁定为满值；因此 {@code essence base} 的
 *       上限设为满值减一，并拒绝修改十绝体玩家的第一空窍基础真元。</li>
 *   <li>⚠ {@link #getSelector} 通过遍历 {@code context.getNodes()} 匹配节点名 {@value #FIRST} 与 {@value #SECOND} 来判
 *       断选窍，同一命令路径上的其他节点严禁复用这两个字面量名称。</li>
 * </ul>
 */

public final class ApertureCommand {

    private ApertureCommand() {}

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
