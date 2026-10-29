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
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
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

/**
 * {@code /gzr aperture}: writes the holder's apertures [空窍]; the B panel reads them.
 *
 * <p>Guarded by a {@code requires(sourceAwakened)} gate (presentation only) AND a per-target gate (data
 * protection). Every value is a literal, so the tree carries its own suggestions and a word like
 * {@code ten_extreme:verdant_great_sun} needs no argument type. {@code rank <1..5|up|down>} may be followed by
 * {@code stage <init|mid|upper|peak|up|down>}, and both land in one write. An optional {@code first|second}
 * before {@code [targets]} picks the aperture (default the first); {@code essence refill} without it fills
 * every aperture, the distilled pool of a distilling one included.
 *
 * <p>{@code talent set} acts on the first aperture alone: a grade re-rolls its base essence and drops any
 * Ten Extremes physique, and {@code ten_extreme:<physique>} stores that physique, whose change event sets
 * the full base. {@code essence base} therefore stops below the full base and refuses a Ten Extremes first
 * aperture.
 *
 * <p>⚠ Readers find the chosen words by walking {@code context.getNodes()} ({@link #getWordAfter}: the literal
 * parsed right after a named node, or with {@code null} the selector literal if one was typed); {@code stage} and
 * the selector literals are matched by name, so no other node in this subtree may reuse them.
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
    private static final String UP = "up";
    private static final String DOWN = "down";
    static final String TEN_EXTREME = "ten_extreme:";
    static final int MAX_COMMAND_BASE = Aperture.MAX_BASE - 1;
    private static final String FAILED_INDEX = "guzhenren.command.failed.aperture_index";
    static final String FAILED_BASE_EXTREME = "guzhenren.command.failed.base_extreme";
    private static final Map<String, Stage> STAGES = Map.of(
            "init", Stage.INIT, "mid", Stage.MIDDLE, "upper", Stage.UPPER, "peak", Stage.PEAK);
    private static final List<String> STAGE_WORDS = List.of("init", "mid", "upper", "peak", UP, DOWN);

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        LiteralArgumentBuilder<CommandSourceStack> rank = Commands.literal(RANK);
        for (int i = 1; i <= Rank.getSettable().length; i++) rank.then(buildRankWord(String.valueOf(i)));
        rank.then(buildRankWord(UP)).then(buildRankWord(DOWN));

        return Commands.literal("aperture").requires(AwakenedGate::isSourceAwakened)
                .then(rank)
                .then(buildStage())
                .then(buildTalent())
                .then(buildEssence());
    }

    //region rank and stage
    private static ArgumentBuilder<CommandSourceStack, ?> buildRankWord(String word) {
        return withSelector(Commands.literal(word), CmdAperture::grade).then(buildStage());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildStage() {
        LiteralArgumentBuilder<CommandSourceStack> stage = Commands.literal(STAGE);
        for (String word : STAGE_WORDS) stage.then(withSelector(Commands.literal(word), CmdAperture::grade));
        return stage;
    }

    private static int grade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String rankWord = getWordAfter(context, RANK);
        String stageWord = getWordAfter(context, STAGE);
        return applyOnAperture(context, (player, index) -> {
            Aperture aperture = ApertureService.getAperture(player, index);
            if (rankWord != null) aperture = aperture.withRank(parseRank(rankWord, aperture.rank()));
            if (stageWord != null) aperture = aperture.withStage(parseStage(stageWord, aperture.stage()));
            ApertureService.set(player, index, aperture);
        });
    }

    static Rank parseRank(String word, Rank current) {
        return switch (word) {
            case UP -> current.shift(1);
            case DOWN -> current.shift(-1);
            default -> Rank.getSettable()[Integer.parseInt(word) - 1];
        };
    }

    static Stage parseStage(String word, Stage current) {
        return switch (word) {
            case UP -> current.shift(1);
            case DOWN -> current.shift(-1);
            default -> STAGES.get(word);
        };
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

    //region essence
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

    //region aperture selection
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
        return getWordAfter(context, null) != null;
    }

    private static boolean isSecond(CommandContext<CommandSourceStack> context) {
        return SECOND.equals(getWordAfter(context, null));
    }

    static String getWordAfter(CommandContext<CommandSourceStack> context, String name) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = context.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            String current = nodes.get(i).getNode().getName();
            if (name == null) {
                if (i > 0 && (current.equals(FIRST) || current.equals(SECOND))) return current;
            } else if (current.equals(name) && i + 1 < nodes.size()) {
                return nodes.get(i + 1).getNode().getName();
            }
        }
        return null;
    }
    //endregion

    @FunctionalInterface
    private interface Indexed {

        void apply(ServerPlayer player, int aperture);
    }
}
