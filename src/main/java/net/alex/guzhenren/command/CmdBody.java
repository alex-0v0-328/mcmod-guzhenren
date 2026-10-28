package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.mortal.strength.SkinGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr body}: writes body [肉身] state -- physiques, race, skin, tendons, bones, lifespan and age;
 * {@code /gzr info} reads it.
 *
 * <p>Assembles the body subtree under {@code /gzr body}: physiques, race, the three tempered parts, lifespan and
 * age. The path domain writes live under {@code /gzr path}. All of {@code /gzr body} is ungated -- a
 * mortal ages and changes form too. The Ten Extremes physique is not written here: {@code /gzr aperture talent}
 * owns it, because it decides the first aperture's base essence.
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
    private static final Physique[] PHYSIQUES = Arrays.stream(Physique.values())
            .filter(physique -> physique != Physique.EXTREME).toArray(Physique[]::new);

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("body")
                .then(buildPhysique())
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

    private static ArgumentBuilder<CommandSourceStack, ?> buildPhysique() {
        return Commands.literal("physique")
                .then(physiqueAction("add", BodyService::addPhysique))
                .then(physiqueAction("remove", BodyService::removePhysique));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> physiqueAction(
            String literal, ModCommandSupport.EnumOperation<Physique> operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, PHYSIQUES), context -> {
                    Physique value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, PHYSIQUES);
                    return ModCommandSupport.apply(context, player -> operation.apply(player, value));
                }));
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
}
