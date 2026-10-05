package net.alex.guzhenren.gameplay.aperture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.command.ModEnumArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * {@code /gzr aperture [1..2]}: writes one aperture [空窍] of the holder; {@code /gzr info} reads it.
 *
 * <p>Guarded by a {@code requires(sourceAwakened)} gate (presentation only) AND an
 * {@code applyOnAperture} per-target gate (data protection). An optional integer index right after the
 * literal picks the aperture (1-based, default the first) -- safe next to a literal, unlike a word
 * argument. Offers graded setters for rank, stage, and talent, the essence subtree with {@code base}/
 * {@code current}/{@code distilled} and their {@code refill} where applicable.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModCommandSupport
 * @since 1.0.0
 */

public final class CmdAperture {

    private CmdAperture() {}

    private static final String ARG_APERTURE = "index";
    private static final String FAILED_INDEX = "guzhenren.command.failed.aperture_index";

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("aperture").requires(AwakenedGate::sourceAwakened);
        ArgumentBuilder<CommandSourceStack, ?> indexed = Commands.argument(ARG_APERTURE,
                IntegerArgumentType.integer(1, ApertureData.MAX_APERTURES));

        attachWrites(root);
        attachWrites(indexed);
        root.then(indexed);
        return root;
    }

    private static void attachWrites(ArgumentBuilder<CommandSourceStack, ?> parent) {
        parent.then(graded("rank", Rank.settable(),
                ApertureService::setRank, ApertureService::shiftRank))
                .then(graded("stage", Stage.settable(),
                        ApertureService::setStage, ApertureService::shiftStage))
                .then(graded("talent", Talent.settable(),
                        ApertureService::setTalent, ApertureService::shiftTalent))
                .then(essence());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> essence() {
        return Commands.literal("essence")
                .then(Commands.literal("base")
                        .then(baseSetNode())
                        .then(baseNode("add", ApertureService::addBaseEssence))
                        .then(baseNode("sub",
                                (player, index, value) -> ApertureService.addBaseEssence(player, index, -value))))
                .then(Commands.literal("current")
                        .then(longNode("set", ApertureEssenceService::set))
                        .then(longNode("add",
                                (player, index, value) -> ApertureEssenceService.set(player, index,
                                        ApertureService.aperture(player, index).currentEssence() + value)))
                        .then(longNode("sub",
                                (player, index, value) -> ApertureEssenceService.set(player, index,
                                        ApertureService.aperture(player, index).currentEssence() - value))))
                .then(Commands.literal("distilled")
                        .then(longNode("set", ApertureEssenceService::setDistilled))
                        .then(longNode("add",
                                (player, index, value) -> ApertureEssenceService.setDistilled(player, index,
                                        ApertureService.aperture(player, index).distilledEssence() + value)))
                        .then(longNode("sub",
                                (player, index, value) -> ApertureEssenceService.setDistilled(player, index,
                                        ApertureService.aperture(player, index).distilledEssence() - value))))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> AwakenedGate.applyOnAwakened(context, ApertureEssenceService::refill)));
    }

    //region builders
    private static int apertureOf(CommandContext<CommandSourceStack> context) {
        boolean indexed = context.getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals(ARG_APERTURE));
        return indexed ? IntegerArgumentType.getInteger(context, ARG_APERTURE) - 1
                : ApertureData.PRIMARY;
    }

    private static int applyOnAperture(CommandContext<CommandSourceStack> context, Indexed operation)
            throws CommandSyntaxException {
        int index = apertureOf(context);
        String refused = index == ApertureData.PRIMARY
                ? ModCommandSupport.FAILED_UNAWAKENED : FAILED_INDEX;
        return ModCommandSupport.applyIf(context,
                AwakenedGate.AWAKENED.and(player -> index < ApertureService.get(player).count()),
                refused, player -> operation.apply(player, index));
    }

    @FunctionalInterface
    private interface Indexed {

        void apply(ServerPlayer player, int aperture);
    }

    @FunctionalInterface
    private interface EnumOp<E extends Enum<E>> {

        void apply(ServerPlayer player, int aperture, E value);
    }

    @FunctionalInterface
    private interface IntOp {

        void apply(ServerPlayer player, int aperture, int value);
    }

    @FunctionalInterface
    private interface LongOp {

        void apply(ServerPlayer player, int aperture, long value);
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> graded(
            String literal, E[] settable, EnumOp<E> set, IntOp shift) {
        return Commands.literal(literal)
                .then(Commands.literal("set")
                        .then(ModCommandSupport.withTargets(
                                ModEnumArgument.arg(ModCommandSupport.ARG_VALUE, settable), context -> {
                                    E value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, settable);
                                    return applyOnAperture(context,
                                            (player, aperture) -> set.apply(player, aperture, value));
                                })))
                .then(shiftNode("up", 1, shift))
                .then(shiftNode("down", -1, shift));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> shiftNode(String literal, int delta, IntOp shift) {
        return ModCommandSupport.withTargets(Commands.literal(literal),
                context -> applyOnAperture(context, (player, aperture) -> shift.apply(player, aperture, delta)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> longNode(String literal, LongOp operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                Commands.argument(ModCommandSupport.ARG_VALUE, LongArgumentType.longArg()),
                context -> {
                    long value = LongArgumentType.getLong(context, ModCommandSupport.ARG_VALUE);
                    return applyOnAperture(context, (player, aperture) -> operation.apply(player, aperture, value));
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> baseNode(String literal, IntOp operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                Commands.argument(ModCommandSupport.ARG_VALUE,
                        IntegerArgumentType.integer(-Aperture.MAX_BASE, Aperture.MAX_BASE)),
                context -> {
                    int value = IntegerArgumentType.getInteger(context, ModCommandSupport.ARG_VALUE);
                    return applyOnAperture(context, (player, aperture) -> operation.apply(player, aperture, value));
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> baseSetNode() {
        return Commands.literal("set").then(ModCommandSupport.withTargets(
                Commands.argument(ModCommandSupport.ARG_VALUE,
                        IntegerArgumentType.integer(-Aperture.MAX_BASE, Aperture.MAX_BASE)),
                context -> {
                    int value = IntegerArgumentType.getInteger(context, ModCommandSupport.ARG_VALUE);
                    int index = apertureOf(context);
                    String refused = index == ApertureData.PRIMARY
                            ? ModCommandSupport.FAILED_EXTREME
                            : FAILED_INDEX;
                    return ModCommandSupport.applyIfResult(context,
                            AwakenedGate.AWAKENED.and(player -> index < ApertureService.get(player).count()),
                            refused,
                            player -> ApertureService.setBaseEssence(player, index, value));
                }));
    }
    //endregion
}
