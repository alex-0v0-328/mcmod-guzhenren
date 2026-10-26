package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.mortal.strength.SkinGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * {@code /gzr body}: writes body [肉身] state -- physiques, race, skin, tendons, bones, lifespan and age;
 * {@code /gzr info} reads it.
 *
 * <p>Assembles the body subtree under {@code /gzr body}: physiques, race, the three tempered parts, lifespan and
 * age. The path domain writes live under {@code /gzr path}. All of {@code /gzr body} is ungated -- a
 * mortal ages and changes form too -- except {@code extreme set}, which needs an awakened target. That gate
 * comes in through {@link #buildNode} from the command root, so the body package never names the aperture package.
 *
 * <p>{@code skin set <skin> [seconds]} wears a skin the way its Gu would and projects it at once; a jade skin
 * without {@code seconds} takes its Gu's registered duration, a permanent skin ignores the figure, and
 * {@code native} sheds whatever is worn. {@code tendon set} and {@code bone set} temper without the Gu's
 * replacement gate: an operator may lower a grade or set it back to native.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModCommandSupport
 * @since 1.0.0
 */

public final class CmdBody {

    private CmdBody() {}

    private static final String ARG_SECONDS = "seconds";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode(Predicate<ServerPlayer> awakened) {
        return Commands.literal("body")
                .then(buildPhysique(awakened))
                .then(ModCommandSupport.buildEnumSetNode("race", Race.values(),
                        BodyService::setRace, ModCommandSupport.ANYONE, null))
                .then(buildSkin())
                .then(ModCommandSupport.buildEnumSetNode("tendon", Tendon.values(),
                        BodyService::temperTendon, ModCommandSupport.ANYONE, null))
                .then(ModCommandSupport.buildEnumSetNode("bone", Bone.values(),
                        BodyService::temperBone, ModCommandSupport.ANYONE, null))
                .then(ModCommandSupport.buildCounter("lifespan", BodyService::setLifespan, BodyService::addLifespan))
                .then(ModCommandSupport.buildCounter("age", BodyService::setAge, BodyService::addAge));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildPhysique(Predicate<ServerPlayer> awakened) {
        return Commands.literal("physique")
                .then(enumAction("add", Physique.values(), BodyService::addPhysique,
                        value -> value != Physique.EXTREME, player -> true,
                        ModCommandSupport.FAILED_EXTREME))
                .then(enumAction("remove", Physique.values(), BodyService::removePhysique, value -> true,
                        player -> true, null))
                .then(Commands.literal("extreme")
                        .then(enumAction("set", ExtremePhysique.getSettable(), BodyService::setExtremePhysique,
                                value -> true, awakened,
                                ModCommandSupport.FAILED_UNAWAKENED)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSkin() {
        return Commands.literal("skin").then(Commands.literal("set")
                .then(ModCommandSupport.withTargets(
                        ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, Skin.values()),
                        context -> wearSkin(context, getRegisteredDurationTicks(skinOf(context))))
                        .then(ModCommandSupport.withTargets(
                                Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0)),
                                context -> wearSkin(context,
                                        IntegerArgumentType.getInteger(context, ARG_SECONDS) * Ticks.SECOND)))));
    }

    private static Skin skinOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, Skin.values());
    }

    private static int wearSkin(CommandContext<CommandSourceStack> context, int durationTicks)
            throws CommandSyntaxException {
        Skin skin = skinOf(context);
        return ModCommandSupport.apply(context, player -> {
            if (skin.isNative()) {
                BodyService.shedSkin(player);
            } else {
                BodyService.wearSkin(player, skin, durationTicks);
            }
            SkinService.tickSkin(player);
        });
    }

    /** A jade skin's time when the command names none: what its Gu registers; a permanent skin takes none. */
    static int getRegisteredDurationTicks(Skin skin) {
        return switch (skin) {
            case JADE_SKIN -> ((SkinGuItem) ModItems.JADE_SKIN_GU.get()).getDurationTicks();
            case WHITE_JADE_SKIN -> ((SkinGuItem) ModItems.WHITE_JADE_GU.get()).getDurationTicks();
            default -> 0;
        };
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> enumAction(
            String literal, E[] values, ModCommandSupport.EnumOperation<E> operation,
            Predicate<E> valueAllowed, Predicate<ServerPlayer> allowed, String refusedKey) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, values), context -> {
                    E value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, values);
                    boolean valid = valueAllowed.test(value);
                    Predicate<ServerPlayer> gate = !valid
                            ? player -> false : allowed;
                    String key = !valid
                            ? ModCommandSupport.FAILED_EXTREME : refusedKey;
                    return ModCommandSupport.applyIf(context, gate, key, player -> operation.apply(player, value));
                }));
    }
}
